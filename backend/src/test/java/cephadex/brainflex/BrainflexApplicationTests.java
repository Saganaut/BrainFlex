package cephadex.brainflex;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"MONGO_URI=mongodb://localhost:27017/test",
		"REDIS_HOST=localhost",
		"REDIS_PORT=6379",
		"REDIS_PASSWORD=password"
})
class BrainflexApplicationTests {

	@Test
	void contextLoads() {
	}

}
