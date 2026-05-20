// One uploaded (or embedded) piece of media in a user's media library.
// Generalizes GalleryImage to support audio, video files, and external video
// embeds alongside images. Mirrors GalleryImage's ownership model: ownerId is
// the original uploader; organizationId is null for private assets, or set to
// an org the uploader belongs to so all other members of that org can pick
// from this asset in their slide editors. Editing/deletion stays owner-only —
// org sharing only widens visibility, not write access.
//
// Storage strategy varies by kind:
//   IMAGE       — `variants` is populated with one row per ImageSize tier and
//                 the underlying objects live under media-assets/{id}/{size}.webp
//   AUDIO       — single S3 object at media-assets/{id}/file.{fileExtension}
//                 (passthrough; no transcode in v1). `variants` is empty.
//   VIDEO_FILE  — single S3 object at media-assets/{id}/file.{fileExtension}
//                 (passthrough). `variants` is empty.
//   VIDEO_EMBED — no S3 object. `embedUrl` carries the literal iframe URL
//                 (YouTube/Vimeo). `sizeBytes` is 0; `mimeType` is null.
//
// width/height/durationMs are populated when known (image dimensions, video
// pixel dims, audio + video durations). v1 only fills these for images; the
// audio/video metadata extractors land later.
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.MediaKind;
import lombok.Data;

@Data
@Document(collection = "media_assets")
public class MediaAsset {

    @Id
    private String id;

    private MediaKind kind;

    private String name;
    private String ownerId;

    /** Null = private to owner. Set to an org id to share with all org members. */
    private String organizationId;

    /** One entry per ImageSize tier for IMAGE; empty for AUDIO/VIDEO_FILE/VIDEO_EMBED. */
    private List<StoredImageVariant> variants = new ArrayList<>();

    /** File extension for AUDIO + VIDEO_FILE (e.g. "mp3", "m4a", "mp4"). Null for IMAGE (webp implied) and VIDEO_EMBED. */
    private String fileExtension;

    /** VIDEO_EMBED only: the literal iframe URL (YouTube/Vimeo). Null for everything else. */
    private String embedUrl;

    /** Original upload size. 0 for VIDEO_EMBED. */
    private long sizeBytes;

    /** Pixel dimensions when known (IMAGE: largest variant; VIDEO_FILE: source dims). Null for AUDIO + VIDEO_EMBED. */
    private Integer width;
    private Integer height;

    /** Audio + video duration in milliseconds when known. Null for IMAGE and when metadata extraction is unavailable. */
    private Integer durationMs;

    /** Source MIME type (image/jpeg, audio/mpeg, video/mp4, …). Null for VIDEO_EMBED. */
    private String mimeType;

    private String altText;
    private String attribution;
    private String sourceUrl;

    /** Free-form keyword tags for filtering in the picker. Never null. */
    private List<String> tags = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
