package cephadex.brainflex.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cephadex.brainflex.repository.AudienceSubmissionRepository;
import cephadex.brainflex.repository.BestAnswerVoteRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GalleryImageRepository;
import cephadex.brainflex.repository.ShowcaseResultRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.TagRepository;
import cephadex.brainflex.repository.ThemeRepository;
import cephadex.brainflex.repository.UserRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // HealthController's direct dependencies — mocked to simulate up/down scenarios
    @MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private RedisConnection redisConnection;

    // Mocking MongoTemplate replaces the real bean, which would cause Spring Data
    // repositories to NPE during initialization (getConverter() returns null on a
    // Mockito mock). Mocking all repos here prevents that.
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DeckRepository deckRepository;

    @MockitoBean
    private ShowcaseRepository showcaseRepository;

    @MockitoBean
    private ShowcaseResultRepository showcaseResultRepository;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @MockitoBean
    private ThemeRepository themeRepository;

    @MockitoBean
    private TagRepository tagRepository;

    @MockitoBean
    private GalleryImageRepository galleryImageRepository;

    @MockitoBean
    private AudienceSubmissionRepository audienceSubmissionRepository;

    @MockitoBean
    private BestAnswerVoteRepository bestAnswerVoteRepository;

    // GridFsTemplate auto-configuration also reads MongoConverter from the
    // mocked MongoTemplate (getConverter() → null), so mock it here too.
    @MockitoBean
    private GridFsTemplate gridFsTemplate;

    @Test
    void getHealth_WhenAllServicesUp_ReturnsUp() throws Exception {
        when(mongoTemplate.getDb()).thenReturn(mock(MongoDatabase.class));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("CONNECTED"))
                .andExpect(jsonPath("$.redis").value("CONNECTED"));
    }

    @Test
    void getHealth_WhenMongoDown_ReturnsDegraded() throws Exception {
        when(mongoTemplate.getDb()).thenThrow(new MongoException("Connection failed"));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.database").value("DISCONNECTED"))
                .andExpect(jsonPath("$.redis").value("CONNECTED"));
    }

    @Test
    void getHealth_WhenRedisDown_ReturnsDegraded() throws Exception {
        when(mongoTemplate.getDb()).thenReturn(mock(MongoDatabase.class));
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.database").value("CONNECTED"))
                .andExpect(jsonPath("$.redis").value("DISCONNECTED"));
    }

    @Test
    void getHealth_WhenBothDown_ReturnsDegraded() throws Exception {
        when(mongoTemplate.getDb()).thenThrow(new MongoException("Connection failed"));
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.database").value("DISCONNECTED"))
                .andExpect(jsonPath("$.redis").value("DISCONNECTED"));
    }
}