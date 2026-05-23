// Wire shape for an Organization. Carries every chunk-20 profile field plus
// the rotatable inviteCode. The settings page renders all of them; the
// leaderboard / explore views read only the name + logos and ignore the rest,
// so we keep one DTO instead of two views.
package cephadex.brainflex.dto;

import java.time.Instant;
import java.util.List;

import cephadex.brainflex.model.media.StoredImageVariant;
import cephadex.brainflex.model.org.Organization;

public record OrganizationResponse(
        String id,
        String name,
        String ownerId,
        OrganizationPlanResponse plan,
        String description,
        List<StoredImageVariant> logoVariants,
        String websiteUrl,
        String location,
        String emailDomain,
        String inviteCode,
        boolean allowPublicJoin,
        int memberCount,
        String defaultThemeId,
        Instant createdAt,
        Instant updatedAt) {

    public OrganizationResponse(Organization org) {
        this(
                org.getId(),
                org.getName(),
                org.getOwnerId(),
                OrganizationPlanResponse.from(org.getPlan()),
                org.getDescription(),
                org.getLogoVariants() == null ? List.of() : List.copyOf(org.getLogoVariants()),
                org.getWebsiteUrl(),
                org.getLocation(),
                org.getEmailDomain(),
                org.getInviteCode(),
                org.isAllowPublicJoin(),
                org.getMemberCount(),
                org.getDefaultThemeId(),
                org.getCreatedAt(),
                org.getUpdatedAt());
    }
}
