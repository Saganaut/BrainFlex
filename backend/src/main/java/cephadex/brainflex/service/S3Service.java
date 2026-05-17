package cephadex.brainflex.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import cephadex.brainflex.config.S3Properties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties props;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, S3Properties props) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.props = props;
    }

    /**
     * Uploads processed image bytes under a deterministic per-user key.
     * Re-uploading always overwrites the same object so no deletion is needed.
     * Returns a presigned GET URL valid for the configured expiry duration.
     */
    public String uploadProfileImage(String userId, byte[] imageBytes) {
        String key = "profile-images/" + userId + "/avatar.webp";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .contentType("image/webp")
                        .build(),
                RequestBody.fromBytes(imageBytes));
        return generatePresignedUrl(key);
    }

    public String uploadThemeBackground(String themeId, byte[] imageBytes) {
        String key = "theme-backgrounds/" + themeId + "/bg.webp";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .contentType("image/webp")
                        .build(),
                RequestBody.fromBytes(imageBytes));
        return generatePresignedUrl(key);
    }

    public String uploadThemeLogo(String themeId, byte[] imageBytes) {
        String key = "theme-logos/" + themeId + "/logo.webp";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .contentType("image/webp")
                        .build(),
                RequestBody.fromBytes(imageBytes));
        return generatePresignedUrl(key);
    }

    /** Returns the deterministic S3 key for a gallery image. Exposed so the
     *  controller can persist the key alongside the presigned URL — the URL
     *  expires, the key doesn't, so re-reads can refresh the URL on demand. */
    public String galleryImageKey(String imageId) {
        return "gallery-images/" + imageId + "/image.webp";
    }

    public String uploadGalleryImage(String imageId, byte[] imageBytes) {
        String key = galleryImageKey(imageId);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .contentType("image/webp")
                        .build(),
                RequestBody.fromBytes(imageBytes));
        return generatePresignedUrl(key);
    }

    /** Re-generates a presigned GET URL for a previously uploaded key. Used
     *  when listing gallery images: the stored URL has likely expired since
     *  upload time, but the key is stable. */
    public String refreshPresignedUrl(String key) {
        return generatePresignedUrl(key);
    }

    public void deleteObject(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .build());
    }

    private String generatePresignedUrl(String key) {
        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(props.presignedUrlExpiry()))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(props.bucket())
                                .key(key)
                                .build())
                        .build()).url().toString();
    }
}
