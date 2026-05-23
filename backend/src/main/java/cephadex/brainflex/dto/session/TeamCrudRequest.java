/**
 * Body for host-side team create/update endpoints. Both fields are optional
 * on update so the host can rename without recoloring (or vice versa). On
 * create, the service supplies defaults when either is missing.
 */
package cephadex.brainflex.dto.session;

import jakarta.validation.constraints.Size;

public record TeamCrudRequest(
        @Size(max = 60) String name,
        @Size(max = 24) String color) {
}
