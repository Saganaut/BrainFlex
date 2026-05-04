package cephadex.brainflex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "s3")
public record S3Properties(
        @DefaultValue("http://localhost:3900") String endpoint,
        @DefaultValue("garage") String region,
        @DefaultValue("brainflex-images") String bucket,
        String accessKey,
        String secretKey,
        @DefaultValue("604800") long presignedUrlExpiry) {
}
