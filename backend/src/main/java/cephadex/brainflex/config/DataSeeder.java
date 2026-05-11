// Loads seed data from src/main/resources/seed/*.json on first startup (when the collection is empty).
// To add a new collection: drop a JSON array file in resources/seed/ and call seedCollection() below.
package cephadex.brainflex.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.QuestionRepository;
import cephadex.brainflex.repository.UserRepository;

@Configuration
public class DataSeeder {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Bean
    @SuppressWarnings("unused")
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            DeckRepository deckRepository,
            QuestionRepository questionRepository) {
        return args -> {
            seedCollection(userRepository, "seed/users.json", User.class);
            seedCollection(deckRepository, "seed/decks.json", Deck.class);
            seedCollection(questionRepository, "seed/questions.json", Question.class);
        };
    }

    private <T> void seedCollection(MongoRepository<T, String> repository, String resourcePath, Class<T> type)
            throws Exception {
        if (repository.count() > 0) return;

        ClassPathResource resource = new ClassPathResource(resourcePath);
        List<T> items = objectMapper.readValue(
                resource.getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, type));

        repository.saveAll(items);
        System.out.println("Seeded " + items.size() + " " + type.getSimpleName() + " records from " + resourcePath);
    }
}
