// POST /api/organizations/join-by-code body. Resolves the invite code to an
// org and adds the caller; the org must have allowPublicJoin=true.
package cephadex.brainflex.dto;

public record JoinByCodeRequest(String inviteCode) {
}
