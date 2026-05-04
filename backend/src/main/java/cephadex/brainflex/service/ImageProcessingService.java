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

    private static final long MAX_UPLOAD_BYTES = 1024L * 1024L;
    private static final int MAX_DIMENSION = 500;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    public byte[] validateAndProcess(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No image provided");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image exceeds the 1 MB size limit");
        }

        byte[] bytes = file.getBytes();

        String detected = detectMimeType(bytes);
        if (!ALLOWED_TYPES.contains(detected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid file type. Only JPEG, PNG, WebP, and GIF are accepted");
        }

        ImmutableImage image = ImmutableImage.loader().fromBytes(bytes);
        if (image.width > MAX_DIMENSION || image.height > MAX_DIMENSION) {
            image = image.bound(MAX_DIMENSION, MAX_DIMENSION);
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
