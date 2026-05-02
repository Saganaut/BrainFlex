package cephadex.brainflex.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mongodb.MongoException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private RedisConnection redisConnection;

    @Test
    void getHealth_WhenAllServicesUp_ReturnsUp() throws Exception {
        when(mongoTemplate.getDb()).thenReturn(null); // Mock to avoid exception
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
        when(mongoTemplate.getDb()).thenReturn(null);
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