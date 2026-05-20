// Discriminator for MediaAsset rows. Distinguishes the four media flavors a
// deck can reference today: images (multi-variant WebP renditions), audio
// files (mp3/m4a passthrough), video files (mp4 passthrough), and external
// video embeds (YouTube/Vimeo URLs — no S3 object).
//
// Storage strategy differs per kind:
//   IMAGE       — five WebP renditions on S3, one per ImageSize tier
//   AUDIO       — single S3 object at the original byte-stream
//   VIDEO_FILE  — single S3 object at the original byte-stream
//   VIDEO_EMBED — no S3 object; the literal embed URL is persisted on the row
package cephadex.brainflex.model.enums;

public enum MediaKind {
    IMAGE,
    AUDIO,
    VIDEO_FILE,
    VIDEO_EMBED
}
