/**
 * A tenant grouping that owns themes, decks, and seat memberships. Most fields
 * here are profile / discovery metadata added by chunk 20 — the auth-relevant
 * bits are {@link #emailDomain} (drives Google-OAuth auto-join when the user's
 * email domain matches an org's domain) and {@link #inviteCode} (the share
 * string used by {@code POST /api/organizations/join-by-code}). Both are
 * rotatable; the controller endpoints are still TODO in chunk 20.
 */
package cephadex.brainflex.model.org;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import cephadex.brainflex.model.media.StoredImageVariant;
import cephadex.brainflex.model.shared.Auditable;

@Data
@Document(collection = "organizations")
public class Organization extends Auditable {

    @Id
    private String id;

    private String name;
    private String ownerId;

    /** Subscription plan held by this organization (seat plan). */
    private OrganizationPlan plan = new OrganizationPlan();

    /** Markdown-formatted description shown on the org landing page, max 1000 chars. */
    private String description;

    /** One entry per ImageSize tier (xs/sm/md/lg/xl) for the org logo. Distinct from
     *  any {@link Theme#getLogoVariants()} the org may use — the logo here is the
     *  branded "this org" identity, while the theme logo is per-session chrome.
     *  Never null; empty until an upload completes. */
    private List<StoredImageVariant> logoVariants = new ArrayList<>();

    /** External website, shown on the org landing page. */
    private String websiteUrl;

    /** Free-form city / region. Surfaced on the org landing page. */
    private String location;

    /** Email domain (e.g. {@code "stanford.edu"}) that, when present on a verified Google
     *  identity, auto-adds the user to this org on first login. Indexed so the OAuth
     *  success handler can do a single lookup per login. Stored lowercase. */
    @Indexed
    private String emailDomain;

    /** Opaque shareable join code. The {@code join-by-code} endpoint accepts a body of
     *  {@code { inviteCode }} and checks this field. Rotatable from the org settings page.
     *  Indexed so the lookup is cheap. */
    @Indexed
    private String inviteCode;

    /** If true, any authenticated user knowing the {@link #inviteCode} can self-serve a
     *  join; if false, the code only works for users explicitly invited (chunk 20 reserves
     *  the explicit-invite flow). */
    private boolean allowPublicJoin = false;

    /** Denormalised member count, bumped/decremented by the membership service. Persisted
     *  to keep the org-list view from N+1ing User documents. */
    private int memberCount = 0;

    /** Theme applied to new members' interactive sessions when they don't override.
     *  Null = no org default, fall back to the user's {@link User#getActiveThemeId()}. */
    private String defaultThemeId;
}
