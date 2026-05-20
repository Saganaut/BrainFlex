// MockMvc coverage for MediaAssetController. Mirrors GalleryControllerTest's
// shape: mock UserService for authentication resolution, mock the repository
// + processing/S3 collaborators, exercise each endpoint. Covers image upload
// (delegates to ImageProcessingService), audio + video file upload
// (delegates to MediaProcessingService), VIDEO_EMBED creation (no S3),
// org-scope enforcement, and owner-only edit/delete.
package cephadex.brainflex.controller;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cephadex.brainflex.model.MediaAsset;
import cephadex.brainflex.model.StoredImageVariant;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.model.element.ImageVariant;
import cephadex.brainflex.model.enums.MediaKind;
import cephadex.brainflex.repository.MediaAssetRepository;
import cephadex.brainflex.service.ImageProcessingService;
import cephadex.brainflex.service.ImageProcessingService.ProcessedVariant;
import cephadex.brainflex.service.MediaProcessingService;
import cephadex.brainflex.service.MediaProcessingService.ProcessedFile;
import cephadex.brainflex.service.S3Service;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class MediaAssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaAssetRepository mediaAssetRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private MediaProcessingService mediaProcessingService;

    @MockitoBean
    private ImageProcessingService imageProcessingService;

    private static User user(String id, String... orgIds) {
        User u = new User();
        u.setId(id);
        u.setOrganizationIds(List.of(orgIds));
        return u;
    }

    private static List<StoredImageVariant> sampleStoredVariants() {
        return List.of(
                new StoredImageVariant(ImageSize.XS, 64, 36),
                new StoredImageVariant(ImageSize.SM, 200, 113),
                new StoredImageVariant(ImageSize.MD, 600, 338),
                new StoredImageVariant(ImageSize.LG, 1200, 675),
                new StoredImageVariant(ImageSize.XL, 2000, 1125));
    }

    private static List<ImageVariant> sampleFreshVariants() {
        return List.of(
                new ImageVariant(ImageSize.XS, "https://fresh/xs", 64, 36),
                new ImageVariant(ImageSize.SM, "https://fresh/sm", 200, 113),
                new ImageVariant(ImageSize.MD, "https://fresh/md", 600, 338),
                new ImageVariant(ImageSize.LG, "https://fresh/lg", 1200, 675),
                new ImageVariant(ImageSize.XL, "https://fresh/xl", 2000, 1125));
    }

    private static MediaAsset imageAsset(String id, String ownerId, String orgId, String name) {
        MediaAsset a = new MediaAsset();
        a.setId(id);
        a.setKind(MediaKind.IMAGE);
        a.setOwnerId(ownerId);
        a.setOrganizationId(orgId);
        a.setName(name);
        a.setVariants(sampleStoredVariants());
        return a;
    }

    private static MediaAsset audioAsset(String id, String ownerId) {
        MediaAsset a = new MediaAsset();
        a.setId(id);
        a.setKind(MediaKind.AUDIO);
        a.setOwnerId(ownerId);
        a.setName("song");
        a.setFileExtension("mp3");
        a.setMimeType("audio/mpeg");
        a.setSizeBytes(1234L);
        return a;
    }

    private static MediaAsset embedAsset(String id, String ownerId, String embedUrl) {
        MediaAsset a = new MediaAsset();
        a.setId(id);
        a.setKind(MediaKind.VIDEO_EMBED);
        a.setOwnerId(ownerId);
        a.setName("video");
        a.setEmbedUrl(embedUrl);
        return a;
    }

    @Test
    void list_ReturnsOwnedAndOrgSharedDeduped() throws Exception {
        User u = user("u1", "orgA");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        when(mediaAssetRepository.findByOwnerId("u1"))
                .thenReturn(List.of(imageAsset("a1", "u1", null, "mine")));
        when(mediaAssetRepository.findByOrganizationId("orgA")).thenReturn(List.of(
                imageAsset("a2", "u2", "orgA", "shared"),
                imageAsset("a1", "u1", "orgA", "own-dup")));
        when(s3Service.refreshMediaAssetImage(anyString(), anyList())).thenReturn(sampleFreshVariants());

        mockMvc.perform(get("/api/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("a1"))
                .andExpect(jsonPath("$[0].kind").value("IMAGE"))
                .andExpect(jsonPath("$[1].id").value("a2"))
                .andExpect(jsonPath("$[0].variants.length()").value(5))
                .andExpect(jsonPath("$[0].variants[4].url").value("https://fresh/xl"));
    }

    @Test
    void list_WithKindFilter_NarrowsRepositoryQuery() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        when(mediaAssetRepository.findByOwnerIdAndKind("u1", MediaKind.AUDIO))
                .thenReturn(List.of(audioAsset("a1", "u1")));
        when(s3Service.refreshMediaAssetFile("a1", "mp3")).thenReturn("https://fresh/audio");

        mockMvc.perform(get("/api/media").param("kind", "AUDIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kind").value("AUDIO"))
                .andExpect(jsonPath("$[0].url").value("https://fresh/audio"))
                .andExpect(jsonPath("$[0].variants.length()").value(0));
    }

    @Test
    void uploadImage_PersistsAndReturnsCreated() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        Map<ImageSize, ProcessedVariant> processed = new EnumMap<>(ImageSize.class);
        for (ImageSize size : ImageSize.values()) {
            processed.put(size, new ProcessedVariant(new byte[] { 1, 2 }, size.targetWidth(), size.targetWidth()));
        }
        when(mediaProcessingService.processImage(any())).thenReturn(processed);
        when(s3Service.uploadMediaAssetImage(anyString(), any())).thenReturn(sampleStoredVariants());
        when(s3Service.refreshMediaAssetImage(anyString(), anyList())).thenReturn(sampleFreshVariants());
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", new byte[] { 1, 2 });

        mockMvc.perform(multipart("/api/media")
                .file(file)
                .param("kind", "IMAGE")
                .param("name", "Hero shot")
                .param("tags", "lotr, frodo")
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("IMAGE"))
                .andExpect(jsonPath("$.name").value("Hero shot"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.variants.length()").value(5))
                .andExpect(jsonPath("$.variants[4].url").value("https://fresh/xl"))
                .andExpect(jsonPath("$.ownerId").value("u1"));
    }

    @Test
    void uploadAudio_PersistsAndReturnsCreated() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        byte[] bytes = new byte[] { 'I', 'D', '3', 0, 0 };
        when(mediaProcessingService.processAudio(any()))
                .thenReturn(new ProcessedFile(bytes, "audio/mpeg", "mp3", bytes.length));
        when(s3Service.refreshMediaAssetFile(anyString(), eq("mp3"))).thenReturn("https://fresh/audio");
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "song.mp3", "audio/mpeg", bytes);

        mockMvc.perform(multipart("/api/media")
                .file(file)
                .param("kind", "AUDIO")
                .param("name", "Theme song")
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("AUDIO"))
                .andExpect(jsonPath("$.name").value("Theme song"))
                .andExpect(jsonPath("$.url").value("https://fresh/audio"))
                .andExpect(jsonPath("$.mimeType").value("audio/mpeg"));

        verify(s3Service).uploadMediaAssetFile(anyString(), eq("mp3"), eq("audio/mpeg"), any());
    }

    @Test
    void uploadVideoFile_PersistsAndReturnsCreated() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        byte[] bytes = new byte[16];
        when(mediaProcessingService.processVideoFile(any()))
                .thenReturn(new ProcessedFile(bytes, "video/mp4", "mp4", bytes.length));
        when(s3Service.refreshMediaAssetFile(anyString(), eq("mp4"))).thenReturn("https://fresh/video");
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", bytes);

        mockMvc.perform(multipart("/api/media")
                .file(file)
                .param("kind", "VIDEO_FILE")
                .param("name", "Trailer")
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("VIDEO_FILE"))
                .andExpect(jsonPath("$.url").value("https://fresh/video"));
    }

    @Test
    void uploadVideoEmbedViaMultipart_RejectedAsBadRequest() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", new byte[] { 0 });
        mockMvc.perform(multipart("/api/media")
                .file(file)
                .param("kind", "VIDEO_EMBED")
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEmbed_YouTubeWatchUrl_NormalisedToEmbedForm() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/media/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\",\"name\":\"Clip\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("VIDEO_EMBED"))
                .andExpect(jsonPath("$.url").value("https://www.youtube.com/embed/dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.sourceUrl").value("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
    }

    @Test
    void createEmbed_VimeoUrl_NormalisedToPlayerForm() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/media/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://vimeo.com/76979871\",\"name\":\"Showreel\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://player.vimeo.com/video/76979871"));
    }

    @Test
    void createEmbed_NonAllowlistedHost_Rejected() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));

        mockMvc.perform(post("/api/media/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://evil.example.com/video/123\",\"name\":\"nope\"}"))
                .andExpect(status().isBadRequest());

        verify(mediaAssetRepository, never()).save(any(MediaAsset.class));
    }

    @Test
    void uploadImage_ToOrgUserNotMember_ReturnsForbidden() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        Map<ImageSize, ProcessedVariant> processed = new EnumMap<>(ImageSize.class);
        processed.put(ImageSize.XS, new ProcessedVariant(new byte[] { 1 }, 64, 64));
        when(mediaProcessingService.processImage(any())).thenReturn(processed);
        when(s3Service.uploadMediaAssetImage(anyString(), any())).thenReturn(sampleStoredVariants());

        MockMultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", new byte[] { 1 });

        mockMvc.perform(multipart("/api/media")
                .file(file)
                .param("kind", "IMAGE")
                .param("name", "x")
                .param("organizationId", "orgB")
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());

        verify(s3Service, never()).uploadMediaAssetImage(anyString(), any());
        verify(mediaAssetRepository, never()).save(any(MediaAsset.class));
    }

    @Test
    void updateAsset_AsNonOwner_ReturnsForbidden() throws Exception {
        User caller = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(mediaAssetRepository.findById("a9")).thenReturn(Optional.of(imageAsset("a9", "u2", null, "not mine")));

        mockMvc.perform(put("/api/media/a9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteImage_AsOwner_RemovesFromS3AndDb() throws Exception {
        User caller = user("u1");
        MediaAsset asset = imageAsset("a1", "u1", null, "mine");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(mediaAssetRepository.findById("a1")).thenReturn(Optional.of(asset));

        mockMvc.perform(delete("/api/media/a1"))
                .andExpect(status().isOk());

        verify(s3Service).deleteMediaAssetImage(eq("a1"), anyList());
        verify(mediaAssetRepository).delete(asset);
    }

    @Test
    void deleteAudio_AsOwner_RemovesFileAndRow() throws Exception {
        User caller = user("u1");
        MediaAsset asset = audioAsset("a2", "u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(mediaAssetRepository.findById("a2")).thenReturn(Optional.of(asset));

        mockMvc.perform(delete("/api/media/a2"))
                .andExpect(status().isOk());

        verify(s3Service).deleteMediaAssetFile("a2", "mp3");
        verify(mediaAssetRepository).delete(asset);
    }

    @Test
    void deleteEmbed_AsOwner_LeavesS3Untouched() throws Exception {
        User caller = user("u1");
        MediaAsset asset = embedAsset("a3", "u1", "https://www.youtube.com/embed/abc");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(mediaAssetRepository.findById("a3")).thenReturn(Optional.of(asset));

        mockMvc.perform(delete("/api/media/a3"))
                .andExpect(status().isOk());

        verify(s3Service, never()).deleteMediaAssetImage(anyString(), anyList());
        verify(s3Service, never()).deleteMediaAssetFile(anyString(), anyString());
        verify(mediaAssetRepository).delete(asset);
    }

    @Test
    void deleteAsset_AsNonOwner_ReturnsForbidden() throws Exception {
        User caller = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(mediaAssetRepository.findById("a9")).thenReturn(Optional.of(imageAsset("a9", "u2", null, "not mine")));

        mockMvc.perform(delete("/api/media/a9"))
                .andExpect(status().isForbidden());

        verify(s3Service, never()).deleteMediaAssetImage(anyString(), anyList());
        verify(mediaAssetRepository, never()).delete(any(MediaAsset.class));
    }

    @Test
    void getAsset_ForOrgSharedAsset_Visible() throws Exception {
        User caller = user("u1", "orgA");
        MediaAsset asset = imageAsset("a4", "u2", "orgA", "shared");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(mediaAssetRepository.findById("a4")).thenReturn(Optional.of(asset));
        when(s3Service.refreshMediaAssetImage(anyString(), anyList())).thenReturn(sampleFreshVariants());

        mockMvc.perform(get("/api/media/a4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a4"))
                .andExpect(jsonPath("$.organizationId").value("orgA"));
    }
}
