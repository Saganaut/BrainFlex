// One image uploaded to a user's media library. Mirrors the Theme ownership
// model: ownerId is the original uploader; organizationId is null for private
// images, or set to an org the uploader belongs to so all other members of
// that org can pick from this image in their slide editors. Editing/deletion
// stays owner-only — org sharing only widens visibility, not write access.
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "gallery_images")
public class GalleryImage {

    @Id
    private String id;

    private String name;
    private String ownerId;

    /** Null = private to owner. Set to an org id to share with all org members. */
    private String organizationId;

    /** Free-form keyword tags for filtering in the picker. Never null. */
    private List<String> tags = new ArrayList<>();

    /** Deterministic S3 key the WebP is stored under (gallery-images/{id}/image.webp). */
    private String s3Key;

    /** Presigned GET URL for the stored object. Regenerated on every read. */
    private String imageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
}
