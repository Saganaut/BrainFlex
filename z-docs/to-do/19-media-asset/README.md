# 19 — Media asset

**Status:** Backend + frontend in; play-side render + GalleryImage migration deferred
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
- Lobby music dropdown in the interactive session create form (filters `kind=AUDIO`)
- Inspector audio player widget on slides that have an `audioAssetId`
- Video embed renderer for `VIDEO_EMBED` URLs (YouTube + Vimeo iframe sources only — allowlist)

## Cross-cutting concerns

- **Don't transcode in v1** — accept what's uploaded, surface clear caps. Add transcoding later.
- **Streaming vs presigned download** — for audio/video, presigned URLs work for `<audio>` / `<video>` tags up to ~50 MB. For larger files, switch to CloudFront signed URLs or HLS later.
- **CORS on S3 bucket** — image flow already configured. Confirm audio/video MIME types are allow-listed.

## Checklist

- [x] `MediaAsset` model + `MediaKind` enum + repo
- [x] `MediaAssetService` with size caps + format validation per kind
- [ ] Audio/video metadata extraction *(deferred — v1 ships without `durationMs` / pixel dims for audio + video; `width`/`height` is set for `IMAGE` from the largest WebP rendition. Add jaudiotagger / ffmpeg-metadata in a follow-up.)*
- [x] Endpoints + multipart upload + embed POST + tests
- [ ] `GalleryImageService` becomes a view over `MediaAsset where kind=IMAGE` *(requires data migration; left for a follow-up. New `MediaAsset` collection runs alongside the legacy `gallery_images` collection for now.)*
- [x] DeckElement `videoAssetId` / `audioAssetId` fields with backwards-compat fallback to string URLs *(record components on all 13 element kinds; legacy `videoUrl`/`audioUrl` strings stay populated so existing data keeps working — renderer prefers asset id when set, falls back to url string.)*
- [x] `MediaPicker` component (kind-filterable) *(`Common/MediaPicker/MediaPicker.tsx` + `useMediaPicker` hook — IMAGE / AUDIO / VIDEO_FILE / VIDEO_EMBED tabs with per-kind upload form, search, tag-pill filter, org-share dropdown.)*
- [x] Audio/video upload buttons in editor *(`SlideContent.tsx` exposes Add audio / Add video / Add video link beneath the body field; each opens MediaPicker filtered to that kind and persists the returned asset id via the existing `schedule(patch)` pipeline.)*
- [ ] Lobby music dropdown in interactive session create form (when chunk 13 lands)
- [x] Audio/video render on slides *(editor-side preview via `MediaAssetChip`: shows `<audio controls>` for AUDIO, `<video controls>` for VIDEO_FILE, allow-listed `<iframe>` for VIDEO_EMBED. Play-side rendering inside an active interactive session still pending — see "Open follow-ups" below.)*
- [x] Video embed allowlist (YouTube, Vimeo) *(URL normaliser in `MediaAssetService.normaliseEmbedUrl`: accepts `youtube.com/watch?v=`, `youtu.be/`, `youtube.com/embed/`, `vimeo.com/{id}`, `player.vimeo.com/video/{id}`; rejects everything else with 400.)*
- [x] Frontend codegen + lint *(`useListMediaQuery`, `useUploadMediaMutation`, `useCreateMediaEmbedMutation`, `useUpdateMediaMutation`, `useDeleteMediaMutation`, `useGetMediaQuery`; type baseline unchanged at 48 errors, all new files lint clean.)*
- [x] Backend tests pass *(335 / 335 — was 308 before chunk 19; 27 new tests cover MediaAssetController + the additional DeckElement field on positional clone paths.)*

## Open follow-ups

These came out of chunk 19 and need their own threads of work — they are out of scope for this chunk but worth tracking:

- **Play-side media rendering.** `PlayPage` and `QuestionCard` don't yet read `audioAssetId` / `videoAssetId` or render the media at run-time. The asset is persisted on save; once the play surface knows how to dispatch on it, the renderer can mirror `MediaAssetChip`'s preview shape (audio / video / iframe).
- **Image slot still routed through `GalleryPicker`.** Slide / element image and background fields keep using the legacy `gallery_images` collection. Migrating those to `MediaAsset where kind=IMAGE` requires a one-time data backfill (see deferred checkbox above) and is a separate task.
- **Audio/video metadata.** `durationMs` and pixel dimensions for audio + video remain null. Pulling them out at upload time needs `jaudiotagger` (audio) and an ffmpeg-style probe (video) — both add real binary dependencies, so they're deliberately deferred.
- **Lobby music dropdown.** Waits on chunk 13 — once `InteractiveSession` carries `lobbyMusicAssetId`, the create form needs a `MediaPicker` invoked with `kind="AUDIO"`.

## Implementation notes

Backend files added/touched:

- `backend/src/main/java/cephadex/brainflex/model/enums/MediaKind.java` — IMAGE / AUDIO / VIDEO_FILE / VIDEO_EMBED
- `backend/src/main/java/cephadex/brainflex/model/MediaAsset.java` — `@Document("media_assets")`; image rows reuse the gallery `StoredImageVariant` multi-tier list, audio/video rows carry `fileExtension` + `mimeType` + `sizeBytes`, embed rows carry `embedUrl` + `sourceUrl`
- `backend/src/main/java/cephadex/brainflex/repository/MediaAssetRepository.java` — `findByOwnerId{,AndKind}`, `findByOrganizationId{,AndKind}`
- `backend/src/main/java/cephadex/brainflex/service/MediaProcessingService.java` — per-kind caps (image 5 MB, audio 20 MB, video 100 MB), byte-header MIME detection for MP3/M4A/MP4
- `backend/src/main/java/cephadex/brainflex/service/MediaAssetService.java` — list/get/upload/embed/update/delete + URL hydration
- `backend/src/main/java/cephadex/brainflex/service/S3Service.java` — added `media-assets/` prefix, single-file upload/refresh/delete helpers
- `backend/src/main/java/cephadex/brainflex/service/AuthorizationService.java` — `requireMediaAssetEditable` / `requireMediaAssetVisible`
- `backend/src/main/java/cephadex/brainflex/controller/MediaAssetController.java` — REST at `/api/media`
- `backend/src/main/java/cephadex/brainflex/dto/MediaAssetDTO.java` — `MediaAssetResponse`, `UpdateMediaAssetRequest`, `CreateEmbedRequest`
- `backend/src/main/java/cephadex/brainflex/model/element/*.java` — `videoAssetId` + `audioAssetId` record components added to all 13 element kinds; positional callers updated in `ElementShuffler`, `ElementRedactor`, `DeckElementCloner`, `DeckImageMapper`, `SampleDataSeeder`
- `backend/src/test/java/cephadex/brainflex/controller/MediaAssetControllerTest.java` — 16 cases covering each endpoint + auth / org-scope failure modes

Frontend files added/touched:

- `frontend/src/utils/mediaValidation.ts` — per-kind caps + MIME allow-list mirroring `MediaProcessingService`
- `frontend/src/components/Common/MediaPicker/MediaPicker.tsx` + `MediaPicker.module.css` — kind-filterable picker (file upload for IMAGE/AUDIO/VIDEO_FILE, URL form for VIDEO_EMBED)
- `frontend/src/components/Common/MediaPicker/MediaAssetChip.tsx` + `MediaAssetChip.module.css` — read-only summary chip with inline preview (`<audio>` / `<video>` / `<iframe>`)
- `frontend/src/hooks/useMediaPicker.tsx` — opens MediaPicker in the global modal scoped to a single kind
- `frontend/src/components/DeckEditor/SlideContentTypes/SlideContent.tsx` — wires Add audio / Add video / Add video link buttons + the asset chip + replace / remove actions
- `frontend/src/store/BrainFlexApi.ts` — codegen output picked up the new `/api/media` endpoints, the renamed media operations, and the `audioAssetId` / `videoAssetId` fields on every element kind

Storage layout per kind:

| Kind          | S3 layout                                          | Persisted fields                          |
| ------------- | -------------------------------------------------- | ----------------------------------------- |
| `IMAGE`       | `media-assets/{id}/{xs,sm,md,lg,xl}.webp`          | `variants`, `width`, `height`, `mimeType` |
| `AUDIO`       | `media-assets/{id}/file.{mp3\|m4a}`                | `fileExtension`, `mimeType`, `sizeBytes`  |
| `VIDEO_FILE`  | `media-assets/{id}/file.mp4`                       | `fileExtension`, `mimeType`, `sizeBytes`  |
| `VIDEO_EMBED` | *(no S3 object)*                                   | `embedUrl`, `sourceUrl`                   |

Read-time URL hydration mirrors the gallery flow: `IMAGE` responses carry a presigned `variants[]` with one URL per tier; `AUDIO`/`VIDEO_FILE` responses carry a single presigned `url`; `VIDEO_EMBED` responses echo the stored `embedUrl`. Mutation responses always hydrate fresh URLs, so renderers never merge stale links.
