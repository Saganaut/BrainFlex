/**
 * Smoke tests for the /api/tags surface. Repositories are mocked via
 * @MockitoBean — read endpoints return the seeded curated taxonomy; admin
 * writes are gated by AdminProperties.
 *
 * The full MongoTemplate mock pattern from HealthControllerTest applies here
 * too: any test that overrides MongoTemplate must also mock every Spring
 * Data repository on the classpath (and GridFsTemplate). The simpler MockMvc
 * setup we use below only mocks the services the controller talks to.
 */
package cephadex.brainflex.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cephadex.brainflex.config.AdminProperties;
import cephadex.brainflex.dto.TagDTO;
import cephadex.brainflex.model.Tag;
import cephadex.brainflex.model.User;
import cephadex.brainflex.service.TagService;
import cephadex.brainflex.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class TagControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TagService tagService;
    @MockitoBean private UserService userService;
    @MockitoBean private AdminProperties adminProperties;

    private User callerUser;

    @BeforeEach
    void setUp() {
        callerUser = new User();
        callerUser.setId("user-1");
        callerUser.setUserName("admin");
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.of(callerUser));
    }

    @Test
    void listTags_DefaultsToAll() throws Exception {
        when(tagService.listAll()).thenReturn(List.of(curatedTag("math", "Math"),
                curatedTag("history", "History")));

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("math"))
                .andExpect(jsonPath("$[1].id").value("history"));
    }

    @Test
    void listTags_WhenCuratedTrue_FiltersByCurated() throws Exception {
        when(tagService.listCurated()).thenReturn(List.of(curatedTag("math", "Math")));

        mockMvc.perform(get("/api/tags").param("curated", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("math"))
                .andExpect(jsonPath("$[0].curated").value(true));
    }

    @Test
    void listTags_WhenSearchProvided_DelegatesToSearch() throws Exception {
        when(tagService.search("mat")).thenReturn(List.of(curatedTag("math", "Math")));

        mockMvc.perform(get("/api/tags").param("search", "mat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("math"));
    }

    @Test
    void getTag_ReturnsTagWithChildren() throws Exception {
        Tag history = curatedTag("history", "History");
        Tag child = curatedTag("history-ww2", "World War II");
        child.setParentTagId("history");
        when(tagService.get("history")).thenReturn(history);
        when(tagService.children("history")).thenReturn(List.of(child));

        mockMvc.perform(get("/api/tags/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("history"))
                .andExpect(jsonPath("$.children[0].id").value("history-ww2"));
    }

    @Test
    void createTag_WhenAdmin_Returns201() throws Exception {
        when(adminProperties.isAdmin(callerUser)).thenReturn(true);
        when(tagService.create(any(TagDTO.CreateTagRequest.class)))
                .thenAnswer(inv -> {
                    TagDTO.CreateTagRequest req = inv.getArgument(0);
                    return curatedTag(req.id() != null ? req.id() : "history", req.displayName());
                });

        TagDTO.CreateTagRequest body = new TagDTO.CreateTagRequest(
                "history", "History", null, null, null, true);

        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("history"));
    }

    @Test
    void createTag_WhenNotAdmin_ForcesCuratedFalseAndReturns201() throws Exception {
        when(adminProperties.isAdmin(callerUser)).thenReturn(false);
        when(tagService.create(any(TagDTO.CreateTagRequest.class)))
                .thenAnswer(inv -> {
                    TagDTO.CreateTagRequest req = inv.getArgument(0);
                    // The controller must pass curated=false even though the
                    // request body asked for true.
                    assertThat(req.curated()).isEqualTo(false);
                    Tag tag = new Tag();
                    tag.setId(req.id() != null ? req.id() : "frontend-tips");
                    tag.setDisplayName(req.displayName());
                    tag.setCurated(false);
                    tag.setCreatedAt(LocalDateTime.now());
                    tag.setUpdatedAt(LocalDateTime.now());
                    return tag;
                });

        TagDTO.CreateTagRequest body = new TagDTO.CreateTagRequest(
                null, "Frontend Tips", null, null, null, true);

        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.curated").value(false));
    }

    @Test
    void deleteTag_WhenAdmin_Returns204() throws Exception {
        when(adminProperties.isAdmin(callerUser)).thenReturn(true);

        mockMvc.perform(delete("/api/tags/history"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTag_WhenNotAdmin_Returns403() throws Exception {
        when(adminProperties.isAdmin(callerUser)).thenReturn(false);

        mockMvc.perform(delete("/api/tags/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyAdminEndpoint_WhenNoRegisteredCaller_Returns403() throws Exception {
        when(userService.resolveRegisteredUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/tags/history"))
                .andExpect(status().isForbidden());
    }

    private static Tag curatedTag(String id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setDisplayName(name);
        tag.setCurated(true);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
        return tag;
    }
}
