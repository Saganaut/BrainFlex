// Per-kind validation for media uploads. Images are delegated to
// ImageProcessingService (multi-variant WebP transcode); audio + video are
// validated by size cap + byte-header MIME detection and passed through
// untouched. v1 does not extract duration / pixel dimensions for audio + video
// — those land in a follow-up once jaudiotagger / ffmpeg-metadata are wired
// up. The processed result carries everything the controller needs to write
// the S3 object and persist the MediaAsset row.
package cephadex.brainflex.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.service.ImageProcessingService.ProcessedVariant;

@Service
public class MediaProcessingService {

    private static final long MAX_AUDIO_BYTES = 20L * 1024L * 1024L;
    private static final long MAX_VIDEO_BYTES = 100L * 1024L * 1024L;

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of("audio/mpeg", "audio/mp4");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4");

    private final ImageProcessingService imageProcessingService;

    public MediaProcessingService(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    /** Validated + transcoded image renditions (one per ImageSize tier). */
    public Map<ImageSize, ProcessedVariant> processImage(MultipartFile file) throws IOException {
        return imageProcessingService.processGalleryImage(file);
    }

    /** Audio passthrough: max 20 MB, MP3 (audio/mpeg) or M4A (audio/mp4). */
    public ProcessedFile processAudio(MultipartFile file) throws IOException {
        return processPassthrough(file, MAX_AUDIO_BYTES, ALLOWED_AUDIO_TYPES, "20 MB", "audio");
    }

    /** Video file passthrough: max 100 MB, MP4 (video/mp4) only in v1. */
    public ProcessedFile processVideoFile(MultipartFile file) throws IOException {
        return processPassthrough(file, MAX_VIDEO_BYTES, ALLOWED_VIDEO_TYPES, "100 MB", "video");
    }

    private ProcessedFile processPassthrough(MultipartFile file, long maxBytes, Set<String> allowedTypes,
            String limitLabel, String kindLabel) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No " + kindLabel + " file provided");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    capitalize(kindLabel) + " file exceeds the " + limitLabel + " size limit");
        }
        byte[] bytes = file.getBytes();
        String detected = detectMimeType(bytes);
        if (!allowedTypes.contains(detected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid " + kindLabel + " file type. Allowed: " + String.join(", ", allowedTypes));
        }
        return new ProcessedFile(bytes, detected, extensionFor(detected), bytes.length);
    }

    public record ProcessedFile(byte[] bytes, String mimeType, String extension, long sizeBytes) {
    }

    private static String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "audio/mpeg" -> "mp3";
            case "audio/mp4" -> "m4a";
            case "video/mp4" -> "mp4";
            default -> "bin";
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // Detects audio/video MIME by file header bytes, not the multipart
    // Content-Type. Covers MP3 (ID3v2 tag or MPEG frame sync) and MP4 ISO BMFF
    // containers (used by both m4a audio and mp4 video). Container brand
    // distinction (m4a vs mp4 video) is not done here — the caller's kind
    // parameter selects between the two allow-lists.
    private String detectMimeType(byte[] b) {
        // ID3v2 tag at start of file → MP3
        if (b.length >= 3 && b[0] == 'I' && b[1] == 'D' && b[2] == '3') {
            return "audio/mpeg";
        }
        // MPEG frame sync (0xFFE0 mask) → MP3 without ID3 tag
        if (b.length >= 2
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xE0) == 0xE0) {
            return "audio/mpeg";
        }
        // ISO BMFF container: bytes 4–7 are "ftyp"
        if (b.length >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            // brand at bytes 8–11. Audio brands start with "M4A"/"M4B"; everything
            // else (isom, mp42, MSNV, …) is treated as video. This split lets a
            // file uploaded as kind=AUDIO require an M4A brand and kind=VIDEO_FILE
            // require a non-M4A brand.
            if (b[8] == 'M' && b[9] == '4' && (b[10] == 'A' || b[10] == 'B')) {
                return "audio/mp4";
            }
            return "video/mp4";
        }
        return "application/octet-stream";
    }
}
