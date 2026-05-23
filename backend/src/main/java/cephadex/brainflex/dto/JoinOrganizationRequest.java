// POST /api/organizations/join body. Adds the caller to an existing org by id;
// idempotent if the caller is already a member.
package cephadex.brainflex.dto;

public record JoinOrganizationRequest(String organizationId) {
}
