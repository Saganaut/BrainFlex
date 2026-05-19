# 19 — Media asset

**Status:** Not started
**Depends on:** Nothing strict; chunk 13 (lobby music) references `mediaAssetId`
**Unblocks:** Audio/video in deck elements; lobby music; richer galleries

## Scope

Today `GalleryImage` is image-only. Generalize it to a `MediaAsset` model that supports images, audio files, video files, and external video embeds. This unlocks:

- Lobby music (chunk 13's `lobbyMusicAssetId`)
- Audio/video in deck elements (replacing the current free-form `videoUrl`/`audioUrl` strings)
- A future media library UI that picks any media type

Keep `GalleryImage` for one release as a view over `MediaAsset` rows where `kind=IMAGE`. Then deprecate.

## New models

```text
MediaAsset                             @Document("media_assets")
  @Id String id
  MediaKind kind                       // IMAGE | AUDIO | VIDEO_FILE | VIDEO_EMBED
  String name
  String ownerUserId
  String organizationId                // nullable
  String s3Key                         // null for VIDEO_EMBED
  String url                           // presigned GET (images/audio/video files); literal embed URL (VIDEO_EMBED)
  long sizeBytes                       // 0 for VIDEO_EMBED
  Integer width, height                // images + video files; null for audio
  Integer durationMs                   // audio + video (file or embed when known); null for image
  String mimeType                      // null for VIDEO_EMBED
  String altText, attribution, sourceUrl
  List<String> tags
  LocalDateTime createdAt
```

```text
MediaKind (enum)
  IMAGE, AUDIO, VIDEO_FILE, VIDEO_EMBED
```

S3 key conventions per kind:

- IMAGE: `media/{userId}/{assetId}.webp` (Scrimage-resized just like today)
- AUDIO: `media/{userId}/{assetId}.{mp3|m4a}` (passthrough at first; transcode later if needed)
- VIDEO_FILE: `media/{userId}/{assetId}.mp4` (passthrough; document size cap clearly)
- VIDEO_EMBED: no S3 object; `url` is the YouTube/Vimeo embed URL

## Backend changes

- `MediaAssetRepository`, `MediaAssetService`
- New `ImageProcessingService` parity: extend processing for audio (validate header + duration via `jaudiotagger` or read mp4 metadata) and video (`net.bramp.ffmpeg` to read metadata; don't transcode at first)
- Size caps (env-configurable):
  - IMAGE: 5 MB
  - AUDIO: 20 MB
  - VIDEO_FILE: 100 MB
- Endpoints (under new `MediaAssetController`):
  - `GET    /api/media?kind=&tag=` — paginated, scoped to owner + org
  - `GET    /api/media/{id}` — one (refreshes presigned URL)
  - `POST   /api/media` — multipart upload; field `kind`; returns the saved asset
  - `POST   /api/media/embed` — body `{ kind: VIDEO_EMBED, url, name }` — for YouTube/Vimeo
  - `PUT    /api/media/{id}` — rename + retag + altText
  - `DELETE /api/media/{id}` — delete S3 object + row
- Backwards compat:
  - `GalleryImage` reads still work — implement `GalleryImageService` as a view over `MediaAsset where kind=IMAGE`
  - The existing image upload endpoints (`POST /api/users/me/profile-image`, `/api/themes/{id}/logo`, `/api/themes/{id}/background`) stay as-is — they don't create `MediaAsset` rows, they write directly to deterministic S3 keys
  - `DeckImageHydrationService` continues to resolve `Image.internalImgId` against `MediaAsset.id` (the row id matches the legacy gallery image id format)
- DeckElement migration: don't change `videoUrl`/`audioUrl` shape on existing records. Add **new** optional fields (`String videoAssetId, audioAssetId`) that point at `MediaAsset` ids. When both are set, prefer the assetId. Plan a follow-up migration to fully replace the string fields.

## Frontend changes

- Generalize the existing image picker into `MediaPicker` that can be invoked with a `kind` filter
- Audio/video upload buttons in the deck editor — "Add audio" / "Add video" / "Add video link"
- Lobby music dropdown in the showcase create form (filters `kind=AUDIO`)
- Inspector audio player widget on slides that have an `audioAssetId`
- Video embed renderer for `VIDEO_EMBED` URLs (YouTube + Vimeo iframe sources only — allowlist)

## Cross-cutting concerns

- **Don't transcode in v1** — accept what's uploaded, surface clear caps. Add transcoding later.
- **Streaming vs presigned download** — for audio/video, presigned URLs work for `<audio>` / `<video>` tags up to ~50 MB. For larger files, switch to CloudFront signed URLs or HLS later.
- **CORS on S3 bucket** — image flow already configured. Confirm audio/video MIME types are allow-listed.

## Checklist

- [ ] `MediaAsset` model + `MediaKind` enum + repo
- [ ] `MediaAssetService` with size caps + format validation per kind
- [ ] Audio/video metadata extraction
- [ ] Endpoints + multipart upload + embed POST + tests
- [ ] `GalleryImageService` becomes a view over `MediaAsset where kind=IMAGE`
- [ ] DeckElement `videoAssetId` / `audioAssetId` fields with backwards-compat fallback to string URLs
- [ ] `MediaPicker` component (kind-filterable)
- [ ] Audio/video upload buttons in editor
- [ ] Lobby music dropdown in showcase create form (when chunk 13 lands)
- [ ] Audio/video render on slides
- [ ] Video embed allowlist (YouTube, Vimeo)
- [ ] Frontend codegen + lint
- [ ] Backend tests pass
