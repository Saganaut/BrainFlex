package cephadex.brainflex.model.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import cephadex.brainflex.model.enums.UserRole;
import lombok.Data;
import cephadex.brainflex.model.media.StoredImageVariant;
import cephadex.brainflex.model.org.Membership;
import cephadex.brainflex.model.shared.Auditable;

@Data // Lombok: generates getters, setters, toString
@Document(collection = "users") // This maps to the 'users' collection in Mongo
@CompoundIndex(name = "stats_points_idx", def = "{'stats.totalPoints': -1}") // Adding index for total points for
                                                                             // leaderboard
public class User extends Auditable {
    @Id
    private String id; // Mongo will auto-generate this ObjectId

    @Indexed(unique = true)
    private String email;
    private String name;
    private String userName;
    /** The handle shown in-game and on profile cards. Distinct from {@link #name} (legal /
     *  Google-supplied) and {@link #userName} (login handle). Read paths should fall back
     *  through {@code displayName -> userName -> name} so legacy users still render. */
    private String displayName;
    @Field("isGuest")
    private boolean guest;

    private String googleId;
    /** Discord snowflake id, set by the Discord OAuth flow. Mutually exclusive
     *  with {@link #googleId} and {@link #microsoftId} — each BrainFlex account
     *  is tied to exactly one external identity. */
    private String discordId;
    /** Microsoft OIDC subject (per-app pairwise id), set by the Microsoft
     *  OAuth flow. See {@link #discordId} for the mutual-exclusion contract. */
    private String microsoftId;
    /** External avatar URL — set by the Google or Discord OAuth flows.
     *  Once a user uploads their own avatar via {@code pictureVariants},
     *  read paths prefer the variants and this field stays null. */
    private String pictureUrl;
    /** One entry per ImageSize tier (xs/sm/md/lg/xl) for uploaded avatars.
     *  Empty means the user hasn't uploaded their own picture; renderers
     *  should fall back to {@link #pictureUrl} (Google OAuth) or a stock
     *  placeholder. Never null. */
    private List<StoredImageVariant> pictureVariants = new ArrayList<>();

    /** Optional pointer to a user-supplied avatar URL (e.g. Gravatar). Distinct from
     *  {@link #pictureUrl} (set by the OAuth provider) and {@link #pictureVariants}
     *  (uploaded to S3). Read paths prefer variants > customAvatarUrl > pictureUrl. */
    private String customAvatarUrl;

    /** Markdown-formatted self-description, max 500 chars. Surfaced on the profile page
     *  and the explore-deck "by Author" hover card. Null until the user fills it in. */
    private String bio;

    /** Free-form city / region. Surfaced on the profile page; never used for routing. */
    private String location;

    /** External website, e.g. portfolio link. Plain string — the frontend is responsible for
     *  http(s) prefixing + rel="noreferrer" on render. */
    private String websiteUrl;

    /** BCP-47 locale used to localise dates, leaderboard copy, and digest emails.
     *  Defaults to English; settable via the profile form. */
    private String locale = "en";

    /** Tag ids (from chunk 01 {@code Tag} model) the user has explicitly opted in to for
     *  Explore personalisation. Distinct from "Decks I've favourited" — this is a soft
     *  signal for ranking, not an access grant. */
    private Set<String> tagInterests = new LinkedHashSet<>();

    /** In-app and email opt-in matrix per {@link cephadex.brainflex.model.enums.NotificationKind}.
     *  Lazily populated — legacy users read null here; consumers should treat null as the
     *  spec defaults (see {@link NotificationPrefs#withDefaults()}). */
    private NotificationPrefs notificationPrefs;

    private PlayerStats stats = new PlayerStats();

    /** Billing/subscription state for this user. Defaults to a FREE membership. */
    private Membership membership = new Membership();

    private boolean newsletter;

    @Field("isClosed")
    private boolean closed;
    private Instant closedAt;

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
    private Instant emailVerifiedAt;

    private Instant lastLogin;
}