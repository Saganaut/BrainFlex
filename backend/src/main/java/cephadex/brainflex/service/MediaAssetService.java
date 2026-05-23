// Orchestrates MediaAsset reads, uploads, embed creation, updates, and
// deletions. Sits between MediaAssetController and the storage / processing
// helpers (S3Service, MediaProcessingService, ImageProcessingService).
//
// Read-time URL hydration mirrors the Gallery + Theme controllers: image
// variants are presigned per ImageSize tier on every list/get, audio + video
// files get one presigned URL each, and VIDEO_EMBED simply echoes the stored
// embedUrl. Persisted MediaAsset rows never carry a live URL — so a renderer
// that holds a stale response gets a fresh URL on its next fetch and a
// re-upload overwrites the same S3 key.
package cephadex.brainflex.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.media.CreateEmbedRequest;
import cephadex.brainflex.dto.media.MediaAssetResponse;
import cephadex.brainflex.dto.media.UpdateMediaAssetRequest;
import cephadex.brainflex.model.image.ImageSize;
import cephadex.brainflex.model.image.ImageVariant;
import cephadex.brainflex.model.enums.MediaKind;
import cephadex.brainflex.model.media.MediaAsset;
import cephadex.brainflex.model.media.StoredImageVariant;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.MediaAssetRepository;
import cephadex.brainflex.service.ImageProcessingService.ProcessedVariant;
import cephadex.brainflex.service.MediaProcessingService.ProcessedFile;

@Service
public class MediaAssetService {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_TAGS = 20;
    private static final int MAX_TAG_LENGTH = 40;
    private static final int MAX_ALT_TEXT_LENGTH = 280;
    private static final int MAX_ATTRIBUTION_LENGTH = 280;

    // Allowed embed hosts. The URL normaliser converts shareable forms
    // (youtu.be / youtube.com/watch / vimeo.com/{id}) into the iframe-ready
    // form before persistence so the renderer can drop the URL straight into
    // an <iframe src=...>.
    private static final Pattern YOUTUBE_WATCH = Pattern.compile(
            "^https?://(?:www\\.)?youtube\\.com/watch\\?(?:[^#]*&)?v=([A-Za-z0-9_-]{6,32})");
    private static final Pattern YOUTUBE_SHORT = Pattern.compile(
            "^https?://youtu\\.be/([A-Za-z0-9_-]{6,32})");
    private static final Pattern YOUTUBE_EMBED = Pattern.compile(
            "^https?://(?:www\\.)?youtube(?:-nocookie)?\\.com/embed/([A-Za-z0-9_-]{6,32})");
    private static final Pattern VIMEO_CANONICAL = Pattern.compile(
            "^https?://(?:www\\.)?vimeo\\.com/(\\d{5,12})");
    private static final Pattern VIMEO_PLAYER = Pattern.compile(
            "^https?://player\\.vimeo\\.com/video/(\\d{5,12})");

    private final MediaAssetRepository repository;
    private final S3Service s3Service;
    private final MediaProcessingService mediaProcessing;

    public MediaAssetService(
            MediaAssetRepository repository,
            S3Service s3Service,
            MediaProcessingService mediaProcessing) {
        this.repository = repository;
        this.s3Service = s3Service;
        this.mediaProcessing = mediaProcessing;
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Owner's assets + every asset shared with any of the caller's orgs, optionally
     * filtered by kind + tag.
     */
    public List<MediaAssetResponse> list(User caller, MediaKind kindFilter, String tagFilter) {
        List<MediaAsset> assets = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MediaAsset a : ownerScopedAssets(caller.getId(), kindFilter)) {
            if (seen.add(a.getId()))
                assets.add(a);
        }
        List<String> orgIds = caller.getOrganizationIds();
        if (orgIds != null) {
            for (String orgId : orgIds) {
                if (orgId == null || orgId.isBlank())
                    continue;
                for (MediaAsset a : orgScopedAssets(orgId, kindFilter)) {
                    if (a.getOwnerId() == null || a.getOwnerId().equals(caller.getId()))
                        continue;
                    if (seen.add(a.getId()))
                        assets.add(a);
                }
            }
        }
        if (tagFilter != null && !tagFilter.isBlank()) {
            String needle = tagFilter.trim().toLowerCase();
            assets.removeIf(a -> a.getTags() == null
                    || a.getTags().stream().noneMatch(t -> t != null && t.toLowerCase().contains(needle)));
        }
        return assets.stream().map(this::toResponse).toList();
    }

    public MediaAssetResponse get(MediaAsset asset) {
        return toResponse(asset);
    }

    // ── Uploads ──────────────────────────────────────────────────────────────

    public MediaAssetResponse uploadImage(
            User caller, MultipartFile file, String name, String tagsCsv, String organizationId, String altText)
            throws IOException {
        Map<ImageSize, ProcessedVariant> processed = mediaProcessing.processImage(file);

        MediaAsset asset = newAsset(caller, MediaKind.IMAGE, name, file.getOriginalFilename(), tagsCsv, organizationId);
        asset.setMimeType("image/webp");
        asset.setSizeBytes(file.getSize());
        asset.setAltText(sanitiseShort(altText, MAX_ALT_TEXT_LENGTH));

        List<StoredImageVariant> stored = s3Service.uploadMediaAssetImage(asset.getId(), processed);
        asset.setVariants(stored);
        // Persist the largest rendition's pixel dimensions as the asset-level
        // size so non-image-aware consumers (e.g. an audio/video grid view)
        // still get a usable hint.
        if (!stored.isEmpty()) {
            StoredImageVariant largest = stored.get(stored.size() - 1);
            asset.setWidth(largest.width());
            asset.setHeight(largest.height());
        }

        return toResponse(repository.save(asset));
    }

    public MediaAssetResponse uploadAudio(
            User caller, MultipartFile file, String name, String tagsCsv, String organizationId)
            throws IOException {
        ProcessedFile processed = mediaProcessing.processAudio(file);
        return uploadFile(caller, MediaKind.AUDIO, processed, file.getOriginalFilename(), name, tagsCsv,
                organizationId);
    }

    public MediaAssetResponse uploadVideoFile(
            User caller, MultipartFile file, String name, String tagsCsv, String organizationId)
            throws IOException {
        ProcessedFile processed = mediaProcessing.processVideoFile(file);
        return uploadFile(caller, MediaKind.VIDEO_FILE, processed, file.getOriginalFilename(), name, tagsCsv,
                organizationId);
    }

    private MediaAssetResponse uploadFile(
            User caller, MediaKind kind, ProcessedFile processed, String originalFilename,
            String name, String tagsCsv, String organizationId) {
        MediaAsset asset = newAsset(caller, kind, name, originalFilename, tagsCsv, organizationId);
        asset.setMimeType(processed.mimeType());
        asset.setSizeBytes(processed.sizeBytes());
        asset.setFileExtension(processed.extension());

        s3Service.uploadMediaAssetFile(asset.getId(), processed.extension(), processed.mimeType(), processed.bytes());

        return toResponse(repository.save(asset));
    }

    // ── Embeds ───────────────────────────────────────────────────────────────

    public MediaAssetResponse createEmbed(User caller, CreateEmbedRequest req) {
        if (req == null || req.url() == null || req.url().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Embed URL is required");
        }
        String embedUrl = normaliseEmbedUrl(req.url().trim());

        MediaAsset asset = newAsset(caller, MediaKind.VIDEO_EMBED, req.name(), null,
                joinTags(req.tags()), req.organizationId());
        asset.setEmbedUrl(embedUrl);
        asset.setSourceUrl(req.url().trim());
        asset.setSizeBytes(0L);

        return toResponse(repository.save(asset));
    }

    // ── Updates / deletes ────────────────────────────────────────────────────

    public MediaAssetResponse update(MediaAsset asset, UpdateMediaAssetRequest req,
            User caller) {
        if (req.name() != null && !req.name().isBlank()) {
            asset.setName(sanitiseName(req.name(), asset.getName()));
        }
        if (req.tags() != null) {
            asset.setTags(sanitiseTags(req.tags()));
        }
        // Empty string clears org sharing; null leaves it unchanged.
        if (req.organizationId() != null) {
            asset.setOrganizationId(resolveOrgScope(caller, req.organizationId()));
        }
        if (req.altText() != null) {
            asset.setAltText(sanitiseShort(req.altText(), MAX_ALT_TEXT_LENGTH));
        }
        if (req.attribution() != null) {
            asset.setAttribution(sanitiseShort(req.attribution(), MAX_ATTRIBUTION_LENGTH));
        }
        if (req.sourceUrl() != null) {
            asset.setSourceUrl(req.sourceUrl().isBlank() ? null : req.sourceUrl().trim());
        }
        return toResponse(repository.save(asset));
    }

    public void delete(MediaAsset asset) {
        switch (asset.getKind()) {
            case IMAGE -> {
                if (asset.getVariants() != null && !asset.getVariants().isEmpty()) {
                    s3Service.deleteMediaAssetImage(asset.getId(), asset.getVariants());
                }
            }
            case AUDIO, VIDEO_FILE -> {
                if (asset.getFileExtension() != null) {
                    s3Service.deleteMediaAssetFile(asset.getId(), asset.getFileExtension());
                }
            }
            case VIDEO_EMBED -> {
                // No S3 object to remove.
            }
        }
        repository.delete(asset);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private MediaAsset newAsset(User caller, MediaKind kind, String name, String fallbackName,
            String tagsCsv, String organizationId) {
        MediaAsset asset = new MediaAsset();
        asset.setId(UUID.randomUUID().toString());
        asset.setKind(kind);
        asset.setOwnerId(caller.getId());
        asset.setOrganizationId(resolveOrgScope(caller, organizationId));
        asset.setName(sanitiseName(name == null ? fallbackName : name, fallbackName));
        asset.setTags(sanitiseTags(parseTags(tagsCsv)));
        return asset;
    }

    MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(asset, refreshVariants(asset), refreshUrl(asset));
    }

    private Map<ImageSize, ImageVariant> refreshVariants(MediaAsset asset) {
        if (asset.getKind() != MediaKind.IMAGE)
            return Map.of();
        if (asset.getVariants() == null || asset.getVariants().isEmpty())
            return Map.of();
        return s3Service.refreshMediaAssetImage(asset.getId(), asset.getVariants());
    }

    private String refreshUrl(MediaAsset asset) {
        return switch (asset.getKind()) {
            case AUDIO, VIDEO_FILE -> asset.getFileExtension() == null
                    ? null
                    : s3Service.refreshMediaAssetFile(asset.getId(), asset.getFileExtension());
            case VIDEO_EMBED -> asset.getEmbedUrl();
            case IMAGE -> null;
        };
    }

    private List<MediaAsset> ownerScopedAssets(String ownerId, MediaKind kindFilter) {
        return kindFilter == null
                ? repository.findByOwnerId(ownerId)
                : repository.findByOwnerIdAndKind(ownerId, kindFilter);
    }

    private List<MediaAsset> orgScopedAssets(String orgId, MediaKind kindFilter) {
        return kindFilter == null
                ? repository.findByOrganizationId(orgId)
                : repository.findByOrganizationIdAndKind(orgId, kindFilter);
    }

    /** Same normalisation rule as GalleryController.resolveOrgScope. */
    private static String resolveOrgScope(User caller, String orgId) {
        if (orgId == null || orgId.isBlank())
            return null;
        List<String> memberships = caller.getOrganizationIds();
        if (memberships == null || !memberships.contains(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only share media with organizations you belong to");
        }
        return orgId;
    }

    private static String sanitiseName(String supplied, String fallback) {
        String candidate = supplied == null || supplied.isBlank() ? fallback : supplied;
        if (candidate == null || candidate.isBlank())
            candidate = "Untitled";
        if (candidate.length() > MAX_NAME_LENGTH) {
            candidate = candidate.substring(0, MAX_NAME_LENGTH);
        }
        return candidate.trim();
    }

    private static List<String> parseTags(String csv) {
        if (csv == null || csv.isBlank())
            return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty())
            return null;
        return String.join(",", tags);
    }

    private static List<String> sanitiseTags(List<String> raw) {
        if (raw == null)
            return new ArrayList<>();
        return raw.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .map(t -> t.length() > MAX_TAG_LENGTH ? t.substring(0, MAX_TAG_LENGTH) : t)
                .distinct()
                .limit(MAX_TAGS)
                .toList();
    }

    private static String sanitiseShort(String raw, int maxLength) {
        if (raw == null)
            return null;
        if (raw.isBlank())
            return null;
        String trimmed = raw.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    static String normaliseEmbedUrl(String url) {
        Matcher m;
        if ((m = YOUTUBE_EMBED.matcher(url)).find()) {
            return "https://www.youtube.com/embed/" + m.group(1);
        }
        if ((m = YOUTUBE_WATCH.matcher(url)).find()) {
            return "https://www.youtube.com/embed/" + m.group(1);
        }
        if ((m = YOUTUBE_SHORT.matcher(url)).find()) {
            return "https://www.youtube.com/embed/" + m.group(1);
        }
        if ((m = VIMEO_PLAYER.matcher(url)).find()) {
            return "https://player.vimeo.com/video/" + m.group(1);
        }
        if ((m = VIMEO_CANONICAL.matcher(url)).find()) {
            return "https://player.vimeo.com/video/" + m.group(1);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Embed URL must be a YouTube or Vimeo link");
    }
}
