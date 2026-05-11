package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.Organization;
import cephadex.brainflex.model.OrganizationPlan;

public class OrganizationDTO {

    public record OrganizationResponse(
            String id,
            String name,
            String ownerId,
            OrganizationPlan plan,
            LocalDateTime createdAt) {

        public OrganizationResponse(Organization org) {
            this(org.getId(), org.getName(), org.getOwnerId(), org.getPlan(), org.getCreatedAt());
        }
    }

    public record CreateOrganizationRequest(String name) {
    }

    public record JoinOrganizationRequest(String organizationId) {
    }
}
