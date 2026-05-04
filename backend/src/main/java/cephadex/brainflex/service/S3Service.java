package cephadex.brainflex.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import cephadex.brainflex.config.S3Properties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
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
