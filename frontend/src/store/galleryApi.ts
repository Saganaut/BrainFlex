// Hand-rolled RTK Query slice for the new /api/gallery endpoints. Lives
// outside BrainFlexApi.ts so the auto-generated file stays untouched until
// the next `npm run generate-api` pulls these endpoints into the canonical
// generated client. Once that happens, switch consumers over to the
// generated hooks and delete this file.
import { emptySplitApi } from "./emptyApi";

export interface GalleryImageResponse {
  id: string;
  name: string;
  ownerId: string;
  organizationId?: string | null;
  tags: string[];
  imageUrl?: string | null;
  createdAt?: string;
}

export interface UpdateGalleryImageRequest {
  name?: string;
  tags?: string[];
  organizationId?: string;
}

const GALLERY_TAG = "GalleryImage" as const;

const galleryApi = emptySplitApi
  .enhanceEndpoints({ addTagTypes: [GALLERY_TAG] })
  .injectEndpoints({
    endpoints: (build) => ({
      listGalleryImages: build.query<GalleryImageResponse[], undefined>({
        query: () => ({ url: "/api/gallery" }),
        providesTags: (result) =>
          result
            ? [
                ...result.map(({ id }) => ({
                  type: GALLERY_TAG,
                  id,
                })),
                { type: GALLERY_TAG, id: "LIST" },
              ]
            : [{ type: GALLERY_TAG, id: "LIST" }],
      }),
      uploadGalleryImage: build.mutation<
        GalleryImageResponse,
        { body: FormData }
      >({
        query: (arg) => ({
          url: "/api/gallery",
          method: "POST",
          body: arg.body,
        }),
        invalidatesTags: [{ type: GALLERY_TAG, id: "LIST" }],
      }),
      updateGalleryImage: build.mutation<
        GalleryImageResponse,
        { id: string; body: UpdateGalleryImageRequest }
      >({
        query: (arg) => ({
          url: `/api/gallery/${arg.id}`,
          method: "PUT",
          body: arg.body,
        }),
        invalidatesTags: (_res, _err, arg) => [
          { type: GALLERY_TAG, id: arg.id },
          { type: GALLERY_TAG, id: "LIST" },
        ],
      }),
      deleteGalleryImage: build.mutation<undefined, { id: string }>({
        query: (arg) => ({
          url: `/api/gallery/${arg.id}`,
          method: "DELETE",
        }),
        invalidatesTags: (_res, _err, arg) => [
          { type: GALLERY_TAG, id: arg.id },
          { type: GALLERY_TAG, id: "LIST" },
        ],
      }),
    }),
  });

export const {
  useListGalleryImagesQuery,
  useUploadGalleryImageMutation,
  useUpdateGalleryImageMutation,
  useDeleteGalleryImageMutation,
} = galleryApi;
