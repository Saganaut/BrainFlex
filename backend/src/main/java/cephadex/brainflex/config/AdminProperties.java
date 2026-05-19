/**
 * Env-driven allowlist of user ids that can perform admin-only writes (creating
 * / editing curated tags being the first such operation).
 *
 * Reads from the {@code APP_ADMIN_USER_IDS} env var via Spring's relaxed
 * binding (comma-separated). The list is read-only after boot; restart to
 * update. This is a stopgap until a proper {@code UserRole.ADMIN} field
 * lands as part of chunk 20.
 */
package cephadex.brainflex.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import cephadex.brainflex.model.User;

@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private List<String> userIds = List.of();

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds == null ? List.of() : userIds;
    }

    public boolean isAdmin(User user) {
        if (user == null || user.getId() == null) return false;
        Set<String> allow = new HashSet<>(userIds);
        return allow.contains(user.getId());
    }
}
