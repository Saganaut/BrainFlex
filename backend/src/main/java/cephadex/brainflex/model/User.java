package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.UserRole;
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
    /** External avatar URL — currently only set by the Google OAuth flow.
     *  Once a user uploads their own avatar via {@code pictureVariants},
     *  read paths prefer the variants and this field stays null. */
    private String pictureUrl;
    /** One entry per ImageSize tier (xs/sm/md/lg/xl) for uploaded avatars.
     *  Empty means the user hasn't uploaded their own picture; renderers
     *  should fall back to {@link #pictureUrl} (Google OAuth) or a stock
     *  placeholder. Never null. */
    private List<StoredImageVariant> pictureVariants = new ArrayList<>();

    private PlayerStats stats = new PlayerStats();

    /** Billing/subscription state for this user. Defaults to a FREE membership. */
    private Membership membership = new Membership();

    private Boolean newsletter;

    private Boolean isClosed;
    private LocalDateTime closedAt;

    /** Organizations this user belongs to. Empty list = personal-only. */
    private List<String> organizationIds = new ArrayList<>();

    /** The id of the user's currently active custom Theme (nullable). */
    private String activeThemeId;

    /** Persisted user grants. Every registered user carries USER; MODERATOR and
     *  ADMIN are added by hand. Distinct from billing-tier / org authorities,
     *  which AuthoritiesService recomputes on every login. */
    private Set<UserRole> roles = EnumSet.of(UserRole.USER);

    /** IANA timezone string (e.g. "America/Los_Angeles"). Auto-detected from
     *  the browser on first login; user-overridable via {@code PATCH /api/users/me}.
     *  Null until set — read paths should fall back to UTC. */
    private String timezone;

    /** Stamped on first authenticated session where Google asserts the email
     *  is verified (i.e. any successful OAuth login). Null for guests and for
     *  legacy registered users who haven't logged in since this field
     *  landed — both backfill on next login. Idempotent: never overwritten. */
    private LocalDateTime emailVerifiedAt;

    private LocalDateTime lastLogin;
    private LocalDateTime createdAt = LocalDateTime.now();
}