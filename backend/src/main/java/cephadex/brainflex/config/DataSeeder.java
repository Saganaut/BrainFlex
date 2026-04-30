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
    @SuppressWarnings("unused")
    CommandLineRunner initDatabase(UserRepository repository) {
        return args -> {
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

                // 6. Samwise Gamgee - The true hero
                User sam = new User();
                sam.setName("Samwise Gamgee");
                sam.setUserName("GardenerOfTheYear");
                sam.setEmail("sam@theshire.me");
                sam.setStats(createStats(25, 2000, 12000, 20)); // Massive streak!
                sam.setCreatedAt(LocalDateTime.now().minusDays(1));

                // 7. Boromir
                User boromir = new User();
                boromir.setName("Boromir of Gondor");
                boromir.setUserName("OneDoesNotSimply");
                boromir.setEmail("boromir@denethor.com");
                boromir.setStats(createStats(20, 3100, 9500, 0));
                boromir.setCreatedAt(LocalDateTime.now().minusDays(15));

                // 8. Galadriel
                User galadriel = new User();
                galadriel.setName("Lady Galadriel");
                galadriel.setUserName("LightOfEarendel");
                galadriel.setEmail("galadriel@lorien.org");
                galadriel.setStats(createStats(60, 5500, 32000, 30)); // Top of the pack
                galadriel.setCreatedAt(LocalDateTime.now().minusYears(1));

                // 9. Gollum
                User gollum = new User();
                gollum.setName("Sméagol");
                gollum.setUserName("Precious_Slinker");
                gollum.setEmail("gollum@caves.me");
                gollum.setStats(createStats(100, 1100, 15000, 1));
                gollum.setCreatedAt(LocalDateTime.now().minusYears(5));

                // 10. Saruman
                User saruman = new User();
                saruman.setName("Saruman the White");
                saruman.setUserName("Sharkey");
                saruman.setEmail("saruman@isengard.com");
                saruman.setStats(createStats(55, 4800, 28000, 14));
                saruman.setCreatedAt(LocalDateTime.now().minusDays(20));

                // 11. Elrond
                User elrond = new User();
                elrond.setName("Elrond Half-elven");
                elrond.setUserName("CouncilChairman");
                elrond.setEmail("elrond@rivendell.me");
                elrond.setStats(createStats(48, 4600, 26000, 10));
                elrond.setCreatedAt(LocalDateTime.now().minusDays(30));

                // 12. Éowyn
                User eowyn = new User();
                eowyn.setName("Éowyn of Rohan");
                eowyn.setUserName("IAmNoMan");
                eowyn.setEmail("eowyn@shieldmaiden.ro");
                eowyn.setStats(createStats(35, 3800, 19500, 9));
                eowyn.setCreatedAt(LocalDateTime.now().minusDays(4));

                // 13. Faramir
                User faramir = new User();
                faramir.setName("Faramir");
                faramir.setUserName("QualityCaptain");
                faramir.setEmail("faramir@osgiliath.gov");
                faramir.setStats(createStats(28, 3200, 16000, 6));
                faramir.setCreatedAt(LocalDateTime.now().minusDays(12));

                // 14. Merry
                User merry = new User();
                merry.setName("Meriadoc Brandybuck");
                merry.setUserName("Merry_Buck");
                merry.setEmail("merry@buckland.me");
                merry.setStats(createStats(15, 1800, 5200, 4));
                merry.setCreatedAt(LocalDateTime.now().minusDays(2));

                // 15. Pippin
                User pippin = new User();
                pippin.setName("Peregrin Took");
                pippin.setUserName("FoolOfATook");
                pippin.setEmail("pippin@greatsmials.me");
                pippin.setStats(createStats(14, 1750, 5100, 2));
                pippin.setCreatedAt(LocalDateTime.now().minusDays(2));

                repository.saveAll(List.of(
                        frodo, gandalf, aragorn, legolas, gimli,
                        sam, boromir, galadriel, gollum, saruman,
                        elrond, eowyn, faramir, merry, pippin));
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