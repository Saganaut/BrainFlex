package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data // Lombok: generates getters, setters, toString
@Document(collection = "users") // This maps to the 'users' collection in Mongo
@CompoundIndex(name = "stats_points_idx", def = "{'stats.totalPoints': -1}") // Adding index for total points for
                                                                             // leaderboard
public class User {
    @Id
    private String id; // Mongo will auto-generate this ObjectId

    @Indexed(unique = true)
    private String email;
    private String name;
    private String userName;
    private Boolean isGuest;

    private String googleId;
    private String pictureUrl;

    private PlayerStats stats = new PlayerStats();

    /** Billing/subscription state for this user. Defaults to a FREE membership. */
    private Membership membership = new Membership();

    private Boolean newsletter;

    private Boolean isClosed;
    private LocalDateTime closedAt;

    /** The organization this user belongs to (nullable). */
    private String organizationId;

    /** The id of the user's currently active custom Theme (nullable). */
    private String activeThemeId;

    private LocalDateTime lastLogin;
    private LocalDateTime createdAt = LocalDateTime.now();
}