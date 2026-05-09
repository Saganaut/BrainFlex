package cephadex.brainflex.dto;

import java.time.LocalDateTime;

import cephadex.brainflex.model.Organization;

public class OrganizationDTO {

    public record OrganizationResponse(
            String id,
            String name,
            String ownerId,
            LocalDateTime createdAt) {

        public OrganizationResponse(Organization org) {
            this(org.getId(), org.getName(), org.getOwnerId(), org.getCreatedAt());
        }
    }

    public record CreateOrganizationRequest(String name) {
    }

    public record JoinOrganizationRequest(String organizationId) {
    }
}
