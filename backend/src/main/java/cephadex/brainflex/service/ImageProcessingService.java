package cephadex.brainflex.service;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

import cephadex.brainflex.model.element.ImageSize;

/**
 * Stateless image processing: validate → resize → convert to WebP, producing
 * one rendition per {@link ImageSize} tier. Designed to be extracted to a
 * Lambda with no changes to this logic.
 *
 * Every upload yields an EnumMap of size → {@link ProcessedVariant}. A
 * variant is omitted from the map when the source is smaller than the
 * previous tier's target width (i.e. we don't upscale and we don't store
 * pixel-identical duplicates at different keys).
 */
@Service
public class ImageProcessingService {

    private static final long MAX_AVATAR_BYTES = 1024L * 1024L;
    private static final long MAX_LOGO_BYTES = 2L * 1024L * 1024L;
    private static final long MAX_BACKGROUND_BYTES = 5L * 1024L * 1024L;

    private static final int MAX_AVATAR_DIMENSION = 500;
    private static final int MAX_LOGO_DIMENSION = 400;
    private static final int MAX_BACKGROUND_DIMENSION = 2000;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_TYPES_NO_GIF = Set.of(
            "image/jpeg", "image/png", "image/webp");

    /** Bytes + actual pixel dimensions of one rendition, ready to upload. */
    public record ProcessedVariant(byte[] bytes, int width, int height) {
    }

    /** Avatar: max 1 MB, max 500×500 source, JPEG/PNG/WebP/GIF → WebP renditions. */
    public Map<ImageSize, ProcessedVariant> processAvatar(MultipartFile file) throws IOException {
        return process(file, MAX_AVATAR_BYTES, MAX_AVATAR_DIMENSION, ALLOWED_TYPES, "1 MB");
    }

    /** Logo: max 2 MB, max 400×400 source, JPEG/PNG/WebP/GIF → WebP renditions. */
    public Map<ImageSize, ProcessedVariant> processLogo(MultipartFile file) throws IOException {
        return process(file, MAX_LOGO_BYTES, MAX_LOGO_DIMENSION, ALLOWED_TYPES, "2 MB");
    }

    /** Background: max 5 MB, max 2000px source on the longest side, JPEG/PNG/WebP → WebP renditions. */
    public Map<ImageSize, ProcessedVariant> processBackground(MultipartFile file) throws IOException {
        return process(file, MAX_BACKGROUND_BYTES, MAX_BACKGROUND_DIMENSION, ALLOWED_TYPES_NO_GIF, "5 MB");
    }

    /** Gallery: max 5 MB, max 2000px source on the longest side, JPEG/PNG/WebP → WebP renditions.
     *  Shares the background tier intentionally — same use-case (slide content). */
    public Map<ImageSize, ProcessedVariant> processGalleryImage(MultipartFile file) throws IOException {
        return process(file, MAX_BACKGROUND_BYTES, MAX_BACKGROUND_DIMENSION, ALLOWED_TYPES_NO_GIF, "5 MB");
    }

    private Map<ImageSize, ProcessedVariant> process(MultipartFile file, long maxBytes, int maxDimension,
            Set<String> allowedTypes, String limitLabel) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No image provided");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image exceeds the " + limitLabel + " size limit");
        }

        byte[] bytes = file.getBytes();

        String detected = detectMimeType(bytes);
        if (!allowedTypes.contains(detected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid file type. Only JPEG, PNG, WebP"
                            + (allowedTypes.contains("image/gif") ? ", and GIF" : "") + " are accepted");
        }

        ImmutableImage source = ImmutableImage.loader().fromBytes(bytes);
        // Clamp the source to the tier's max box before generating renditions.
        // Without this an oversized upload would have its XL rendition created
        // at the raw source size, bypassing the per-tier cap.
        if (source.width > maxDimension || source.height > maxDimension) {
            source = source.bound(maxDimension, maxDimension);
        }

        EnumMap<ImageSize, ProcessedVariant> out = new EnumMap<>(ImageSize.class);
        int sourceWidth = source.width;
        int sourceHeight = source.height;
        int lastWidth = -1;
        for (ImageSize size : ImageSize.values()) {
            int targetWidth = Math.min(size.targetWidth(), sourceWidth);
            // Skip pixel-identical duplicates: if a smaller tier was clamped
            // to the source width, every larger tier collapses to the same
            // bitmap. Store the rendition under the smallest tier only.
            if (targetWidth == lastWidth) continue;
            ImmutableImage scaled = (targetWidth >= sourceWidth)
                    ? source
                    : source.scaleToWidth(targetWidth);
            byte[] encoded = scaled.bytes(WebpWriter.DEFAULT);
            out.put(size, new ProcessedVariant(encoded, scaled.width, scaled.height));
            lastWidth = targetWidth;
            if (targetWidth >= sourceWidth) {
                // Source-sized rendition reached: every later tier would be a
                // duplicate. Stop so the EnumMap doesn't pretend they exist.
                // (Sanity check) sourceHeight is used only as part of the
                // ProcessedVariant payload, not the loop guard.
                break;
            }
        }
        // Guarantee at least one rendition (degenerate but-real images go in
        // under the smallest tier so callers don't have to special-case empty
        // maps). sourceHeight kept on the variant so callers can persist it.
        if (out.isEmpty()) {
            out.put(ImageSize.XS, new ProcessedVariant(source.bytes(WebpWriter.DEFAULT), sourceWidth, sourceHeight));
        }
        return out;
    }

    // Checks the actual file header bytes, not the extension or Content-Type
    // header.
    private String detectMimeType(byte[] b) {
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == '\r' && b[5] == '\n' && (b[6] & 0xFF) == 0x1A && b[7] == '\n') {
            return "image/png";
        }
        if (b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}
