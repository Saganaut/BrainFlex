package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;

import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.OrganizationPlan;
import cephadex.brainflex.model.StoredImageVariant;
import jakarta.validation.constraints.Size;

public class OrganizationDTO {

    /**
     * Wire shape carries every chunk-20 profile field plus the rotatable
     * {@code inviteCode}. The settings page renders all of them; the
     * leaderboard / explore views read only the {@code name} + logos and
     * ignore the rest, so we keep one DTO instead of two views.
     */
    public record OrganizationResponse(
            String id,
            String name,
            String ownerId,
            OrganizationPlan plan,
            String description,
            List<StoredImageVariant> logoVariants,
            String websiteUrl,
            String location,
            String emailDomain,
            String inviteCode,
            boolean allowPublicJoin,
            int memberCount,
            String defaultThemeId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public OrganizationResponse(Organization org) {
            this(
                    org.getId(),
                    org.getName(),
                    org.getOwnerId(),
                    org.getPlan(),
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

    public record CreateOrganizationRequest(String name) {
    }

    public record JoinOrganizationRequest(String organizationId) {
    }

    /**
     * Owner-only partial update. Every field is nullable: null = "client did
     * not send the field" → no-op. Empty string is interpreted as "clear this
     * field" to give the settings UI a way to remove an existing value.
     * {@code allowPublicJoin} is a primitive on the model but Boolean here so
     * the null/false distinction round-trips.
     */
    public record UpdateOrganizationRequest(
            @Size(max = 200) String name,
            @Size(max = 1000) String description,
            @Size(max = 500) String websiteUrl,
            @Size(max = 200) String location,
            @Size(max = 253) String emailDomain,
            Boolean allowPublicJoin,
            String defaultThemeId) {
    }

    /** Body of {@code POST /api/organizations/join-by-code}. */
    public record JoinByCodeRequest(String inviteCode) {
    }
}
