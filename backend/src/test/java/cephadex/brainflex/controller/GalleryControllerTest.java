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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cephadex.brainflex.model.GalleryImage;
import cephadex.brainflex.model.StoredImageVariant;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.ImageSize;
import cephadex.brainflex.model.element.ImageVariant;
import cephadex.brainflex.repository.GalleryImageRepository;
import cephadex.brainflex.service.ImageProcessingService;
import cephadex.brainflex.service.ImageProcessingService.ProcessedVariant;
import cephadex.brainflex.service.S3Service;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class GalleryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GalleryImageRepository galleryImageRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private S3Service s3Service;

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

    private static GalleryImage image(String id, String ownerId, String orgId, String name) {
        GalleryImage img = new GalleryImage();
        img.setId(id);
        img.setOwnerId(ownerId);
        img.setOrganizationId(orgId);
        img.setName(name);
        img.setVariants(sampleStoredVariants());
        return img;
    }

    @Test
    void listImages_ReturnsOwnedAndOrgSharedDeduped() throws Exception {
        User u = user("u1", "orgA");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        when(galleryImageRepository.findByOwnerId("u1"))
                .thenReturn(List.of(image("img1", "u1", null, "mine")));
        when(galleryImageRepository.findByOrganizationId("orgA")).thenReturn(List.of(
                image("img2", "u2", "orgA", "shared"),
                image("img1", "u1", "orgA", "own-shared-dup")));
        when(s3Service.refreshGalleryImage(anyString(), anyList())).thenReturn(sampleFreshVariants());

        mockMvc.perform(get("/api/gallery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("img1"))
                .andExpect(jsonPath("$[1].id").value("img2"))
                .andExpect(jsonPath("$[0].variants.length()").value(5))
                .andExpect(jsonPath("$[0].variants[0].size").value("XS"))
                .andExpect(jsonPath("$[0].variants[4].url").value("https://fresh/xl"));
    }

    @Test
    void uploadImage_PersistsAndReturnsCreated() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        Map<ImageSize, ProcessedVariant> processed = new EnumMap<>(ImageSize.class);
        for (ImageSize size : ImageSize.values()) {
            processed.put(size, new ProcessedVariant(new byte[] { 1, 2 }, size.targetWidth(), size.targetWidth()));
        }
        when(imageProcessingService.processGalleryImage(any())).thenReturn(processed);
        when(s3Service.uploadGalleryImage(anyString(), any())).thenReturn(sampleStoredVariants());
        when(s3Service.refreshGalleryImage(anyString(), anyList())).thenReturn(sampleFreshVariants());
        when(galleryImageRepository.save(any(GalleryImage.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("image", "pic.png", "image/png", new byte[] { 1, 2 });

        mockMvc.perform(multipart("/api/gallery")
                .file(file)
                .param("name", "Hero shot")
                .param("tags", "lotr, frodo, ring")
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hero shot"))
                .andExpect(jsonPath("$.tags.length()").value(3))
                .andExpect(jsonPath("$.variants.length()").value(5))
                .andExpect(jsonPath("$.variants[4].url").value("https://fresh/xl"))
                .andExpect(jsonPath("$.ownerId").value("u1"));
    }

    @Test
    void uploadImage_ToOrgUserNotMember_ReturnsForbidden() throws Exception {
        User u = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(u));
        Map<ImageSize, ProcessedVariant> processed = new EnumMap<>(ImageSize.class);
        processed.put(ImageSize.XS, new ProcessedVariant(new byte[] { 1 }, 64, 64));
        when(imageProcessingService.processGalleryImage(any())).thenReturn(processed);

        MockMultipartFile file = new MockMultipartFile("image", "pic.png", "image/png", new byte[] { 1 });

        mockMvc.perform(multipart("/api/gallery")
                .file(file)
                .param("name", "x")
                .param("organizationId", "orgB")
                .with(req -> { req.setMethod("POST"); return req; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());

        verify(s3Service, never()).uploadGalleryImage(anyString(), any());
        verify(galleryImageRepository, never()).save(any(GalleryImage.class));
    }

    @Test
    void updateImage_AsNonOwner_ReturnsForbidden() throws Exception {
        User caller = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(galleryImageRepository.findById("img9"))
                .thenReturn(Optional.of(image("img9", "u2", null, "not mine")));

        mockMvc.perform(put("/api/gallery/img9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteImage_AsOwner_RemovesFromS3AndDb() throws Exception {
        User caller = user("u1");
        GalleryImage img = image("img1", "u1", null, "mine");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(galleryImageRepository.findById("img1")).thenReturn(Optional.of(img));

        mockMvc.perform(delete("/api/gallery/img1"))
                .andExpect(status().isOk());

        verify(s3Service).deleteGalleryImage(eq("img1"), anyList());
        verify(galleryImageRepository).delete(img);
    }

    @Test
    void deleteImage_AsNonOwner_ReturnsForbidden() throws Exception {
        User caller = user("u1");
        when(userService.resolveRegisteredUser(any(Authentication.class))).thenReturn(Optional.of(caller));
        when(galleryImageRepository.findById("img9"))
                .thenReturn(Optional.of(image("img9", "u2", null, "not mine")));

        mockMvc.perform(delete("/api/gallery/img9"))
                .andExpect(status().isForbidden());

        verify(s3Service, never()).deleteGalleryImage(anyString(), anyList());
        verify(galleryImageRepository, never()).delete(any(GalleryImage.class));
    }
}
