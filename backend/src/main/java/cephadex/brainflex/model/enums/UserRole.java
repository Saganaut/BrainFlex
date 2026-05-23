/**
 * Authoritative role flags carried on {@link cephadex.brainflex.model.user.User#getRoles()}.
 *
 * Distinct from the tier / org authorities derived in {@code AuthoritiesService}:
 * those are billing-state-driven and recomputed on every login, while these are
 * persisted moderator/admin grants edited by a human. Every registered user
 * carries at least {@link #USER}; {@link #MODERATOR} and {@link #ADMIN} are
 * added explicitly and emit {@code ROLE_MODERATOR} / {@code ROLE_ADMIN}
 * authorities through {@code AuthoritiesService}.
 */
package cephadex.brainflex.model.enums;

public enum UserRole {
    USER,
    MODERATOR,
    ADMIN
}
