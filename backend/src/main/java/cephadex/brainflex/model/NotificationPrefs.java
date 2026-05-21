/**
 * Embedded user preferences governing which {@link cephadex.brainflex.model.enums.NotificationKind}
 * categories the user receives in-app and via email. Lives on {@link User#getNotificationPrefs()}.
 *
 * The two per-kind maps are populated by {@link #withDefaults()} so a freshly constructed
 * preferences object opts the user in to every in-app channel (the bell is cheap) and into the
 * three high-signal email kinds — interactive session invites, collaborator invites, and org
 * invites — that the spec calls out by name. Every other email kind defaults off, leaving the
 * weekly digest as the catch-all for the rest. Existing kinds added after a user record was
 * persisted will read as null in the map; consumers should treat {@code null} as "use the
 * spec default for this kind" via {@link #inAppEnabled(cephadex.brainflex.model.enums.NotificationKind)}
 * / {@link #emailEnabled(cephadex.brainflex.model.enums.NotificationKind)}.
 *
 * {@code marketingEmail} is left null in the bare constructor so callers can mirror the
 * existing {@code User.newsletter} value when they materialise the prefs for the first time
 * (the spec phrases it "default = User.newsletter"). {@code weeklyDigestEmail} defaults to true.
 */
package cephadex.brainflex.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import cephadex.brainflex.model.enums.NotificationKind;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPrefs {

    /** Kinds whose email channel is on by default. Every other kind starts opted out. */
    private static final Set<NotificationKind> EMAIL_DEFAULT_ON = Set.of(
            NotificationKind.INTERACTIVE_SESSION_INVITE,
            NotificationKind.COLLAB_INVITE,
            NotificationKind.ORG_INVITE);

    private Map<NotificationKind, Boolean> inApp = new EnumMap<>(NotificationKind.class);
    private Map<NotificationKind, Boolean> email = new EnumMap<>(NotificationKind.class);

    private boolean weeklyDigestEmail = true;

    /**
     * Mirrors {@code User.newsletter} on first materialisation. Stored as Boolean so the
     * "not yet decided" state survives a round trip through the persisted document.
     */
    private Boolean marketingEmail;

    /**
     * Returns a fresh {@code NotificationPrefs} with the per-kind maps seeded to the spec
     * defaults. Use this when promoting a legacy user record that has no embedded prefs yet,
     * or when constructing prefs for a brand-new user.
     */
    public static NotificationPrefs withDefaults() {
        NotificationPrefs prefs = new NotificationPrefs();
        for (NotificationKind kind : NotificationKind.values()) {
            prefs.inApp.put(kind, Boolean.TRUE);
            prefs.email.put(kind, EMAIL_DEFAULT_ON.contains(kind));
        }
        return prefs;
    }

    /** True if the user wants in-app notifications for {@code kind}; falls back to the spec
     *  default (always on) when the map has no entry — keeps newly-added kinds opt-in by default. */
    public boolean inAppEnabled(NotificationKind kind) {
        Boolean v = inApp.get(kind);
        return v == null ? true : v;
    }

    /** True if the user wants email notifications for {@code kind}; falls back to the spec
     *  default (only the three invite kinds are on) when the map has no entry. */
    public boolean emailEnabled(NotificationKind kind) {
        Boolean v = email.get(kind);
        return v == null ? EMAIL_DEFAULT_ON.contains(kind) : v;
    }
}
