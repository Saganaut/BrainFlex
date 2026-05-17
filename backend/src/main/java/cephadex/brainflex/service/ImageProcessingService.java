package cephadex.brainflex.service;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

/**
 * Stateless image processing: validate → resize → convert to WebP.
 * Designed to be extracted to a Lambda with no changes to this logic.
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

    /** Avatar: max 1 MB, max 500×500, JPEG/PNG/WebP/GIF → WebP. */
    public byte[] validateAndProcess(MultipartFile file) throws IOException {
        return process(file, MAX_AVATAR_BYTES, MAX_AVATAR_DIMENSION, ALLOWED_TYPES, "1 MB");
    }

    /** Logo: max 2 MB, max 400×400, JPEG/PNG/WebP/GIF → WebP. */
    public byte[] validateAndProcessLogo(MultipartFile file) throws IOException {
        return process(file, MAX_LOGO_BYTES, MAX_LOGO_DIMENSION, ALLOWED_TYPES, "2 MB");
    }

    /** Background: max 5 MB, max 2000px on longest side, JPEG/PNG/WebP → WebP. */
    public byte[] validateAndProcessBackground(MultipartFile file) throws IOException {
        return process(file, MAX_BACKGROUND_BYTES, MAX_BACKGROUND_DIMENSION, ALLOWED_TYPES_NO_GIF, "5 MB");
    }

    /** Gallery: max 5 MB, max 2000px on longest side, JPEG/PNG/WebP → WebP.
     *  Shares the background tier intentionally — same use-case (slide content). */
    public byte[] validateAndProcessGalleryImage(MultipartFile file) throws IOException {
        return process(file, MAX_BACKGROUND_BYTES, MAX_BACKGROUND_DIMENSION, ALLOWED_TYPES_NO_GIF, "5 MB");
    }

    private byte[] process(MultipartFile file, long maxBytes, int maxDimension,
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

        ImmutableImage image = ImmutableImage.loader().fromBytes(bytes);
        if (image.width > maxDimension || image.height > maxDimension) {
            image = image.bound(maxDimension, maxDimension);
        }

        return image.bytes(WebpWriter.DEFAULT);
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
