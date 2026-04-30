package cephadex.brainflex.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cephadex.brainflex.model.PlayerStats;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository repository) {
        return args -> {
            // Check if we already have data to avoid duplicates
            if (repository.count() == 0) {
                System.out.println("Seeding Middle-earth users into the database...");

                // 1. Frodo Baggins
                User frodo = new User();
                frodo.setName("Frodo Baggins");
                frodo.setUserName("RingBearer99");
                frodo.setEmail("frodo@shire.me");
                frodo.setStats(createStats(12, 1500, 4500, 3));
                frodo.setCreatedAt(LocalDateTime.now());

                // 2. Gandalf
                User gandalf = new User();
                gandalf.setName("Gandalf the Grey");
                gandalf.setUserName("Mithrandir");
                gandalf.setEmail("gandalf@istari.com");
                gandalf.setStats(createStats(50, 5000, 25000, 15));
                gandalf.setCreatedAt(LocalDateTime.now().minusDays(10));

                // 3. Aragorn
                User aragorn = new User();
                aragorn.setName("Aragorn II Elessar");
                aragorn.setUserName("Strider");
                aragorn.setEmail("king@gondor.gov");
                aragorn.setStats(createStats(30, 3500, 18000, 8));
                aragorn.setCreatedAt(LocalDateTime.now().minusDays(5));

                // 4. Legolas
                User legolas = new User();
                legolas.setName("Legolas Greenleaf");
                legolas.setUserName("PrinceOfMirkwood");
                legolas.setEmail("legolas@woodland.realm");
                legolas.setStats(createStats(45, 4200, 22000, 12));
                legolas.setCreatedAt(LocalDateTime.now().minusDays(7));

                // 5. Gimli
                User gimli = new User();
                gimli.setName("Gimli Son of Glóin");
                gimli.setUserName("AxeMaster");
                gimli.setEmail("gimli@lonelymountain.com");
                gimli.setStats(createStats(44, 4150, 21500, 11)); // Just behind Legolas!
                gimli.setCreatedAt(LocalDateTime.now().minusDays(7));

                repository.saveAll(List.of(frodo, gandalf, aragorn, legolas, gimli));
                System.out.println("Middle-earth has arrived in BrainFlex!");
            }
        };
    }

    // Helper method to keep the code clean
    private PlayerStats createStats(int games, int high, int total, int streak) {
        PlayerStats stats = new PlayerStats();
        stats.setGamesPlayed(games);
        stats.setHighscore(high);
        stats.setTotalPoints(total);
        stats.setCurrentStreak(streak);
        return stats;
    }
}