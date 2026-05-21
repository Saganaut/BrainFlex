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

    /** Native pixel dimensions of the original upload — kept separately from per-variant
     *  dimensions so the picker can show "1920×1080 original" without resolving an XL
     *  variant. Null on legacy rows uploaded before chunk 20 lands a backfill. */
    private Integer width;
    private Integer height;

    /** Accessibility caption — surfaced as the {@code alt} attribute when an image is
     *  rendered into a slide. Null until the uploader fills it in. */
    private String altText;

    /** Free-form attribution string ("Photo by X on Unsplash"). Renderers display this
     *  beneath the image in the legal-required citation slot. */
    private String attribution;

    /** Original source URL if the image was harvested from a CC-licensed source
     *  (Unsplash, Wikimedia, etc.). Lets the gallery link back to provenance. */
    private String sourceUrl;

    /** MIME type of the original upload (e.g. {@code "image/png"}). Mostly informational
     *  — the stored renditions are always WebP. */
    private String mimeType;

    /** Filename the user uploaded with. Kept for the "downloaded original" affordance and
     *  for human-readable rows in admin tooling. */
    private String originalFileName;

    /** Size of the original upload in bytes. {@code 0} means unknown (legacy rows). */
    private long sizeBytes;

    private LocalDateTime createdAt = LocalDateTime.now();
}
