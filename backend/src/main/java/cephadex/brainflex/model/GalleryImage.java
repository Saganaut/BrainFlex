// One image uploaded to a user's media library. Mirrors the Theme ownership
// model: ownerId is the original uploader; organizationId is null for private
// images, or set to an org the uploader belongs to so all other members of
// that org can pick from this image in their slide editors. Editing/deletion
// stays owner-only — org sharing only widens visibility, not write access.
//
// Each image is stored as five WebP renditions on S3 (one per ImageSize tier)
// so renderers can pick the right resolution for the slot they're filling.
// We persist only the per-variant metadata (size + actual pixel dimensions);
// the S3 key for each rendition is derived from the image id and size enum
// via S3Service.galleryImageKey(id, size).
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

    /** One entry per ImageSize tier (xs/sm/md/lg/xl). Never null after upload. */
    private List<StoredImageVariant> variants = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
