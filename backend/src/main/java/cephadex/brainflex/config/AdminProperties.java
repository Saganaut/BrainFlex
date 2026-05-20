/**
 * Resolves whether a {@link User} carries admin grants.
 *
 * Earlier iterations read an env-driven allowlist ({@code APP_ADMIN_USER_IDS});
 * chunk 20 replaced that with a persisted {@code roles} field on User. This
 * helper preserves the original call shape ({@code isAdmin(user)}) so the
 * six existing call sites — and their tests — don't have to change. New
 * code that just needs forbid-or-allow gating should reach for
 * {@code @PreAuthorize("hasRole('ADMIN')")} on the controller method
 * directly; this helper is for the cases that branch on admin-ness inside
 * the method body (e.g. shaping a CreateTagRequest's curated flag).
 */
package cephadex.brainflex.config;

import org.springframework.stereotype.Component;

import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.UserRole;

@Component
public class AdminProperties {

    public boolean isAdmin(User user) {
        if (user == null) return false;
        var roles = user.getRoles();
        return roles != null && roles.contains(UserRole.ADMIN);
    }
}
