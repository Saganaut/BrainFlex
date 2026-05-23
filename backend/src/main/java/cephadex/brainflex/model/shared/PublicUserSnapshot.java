/**
 * Public-safe slice of {@link UserSnapshot} — name, picture, guest flag — with
 * no {@code userId}. Used on every InteractiveSession broadcast where a
 * client should be able to render another player's display fields without
 * learning their real account id.
 *
 * Pair with a session-scoped {@code playerId} on the surrounding DTO for any
 * actions or references (boot, team-move, vote tally) — the {@code playerId}
 * is the public handle; this snapshot is the public chrome.
 */
package cephadex.brainflex.model.shared;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PublicUserSnapshot(
        String name,
        String pictureUrl,
        boolean guest) {

    public static PublicUserSnapshot from(UserSnapshot snapshot) {
        if (snapshot == null) return null;
        return new PublicUserSnapshot(snapshot.name(), snapshot.pictureUrl(), snapshot.guest());
    }
}
