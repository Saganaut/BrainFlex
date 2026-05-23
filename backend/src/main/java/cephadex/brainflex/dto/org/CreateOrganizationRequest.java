// POST /api/organizations body. Only the name is required at creation time;
// every other profile field is set later via UpdateOrganizationRequest.
package cephadex.brainflex.dto.org;

public record CreateOrganizationRequest(String name) {
}
