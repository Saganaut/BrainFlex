/**
 * Denormalized snapshot of a user as they were when a row was written.
 *
 * Embedded everywhere a row needs to render the writer's display fields after
 * the writer might have been renamed, removed, or otherwise mutated: deck
 * comments, interactive-session chat, reactions, audience submissions,
 * notifications, in-session players, post-game placements, and game-history
 * rows. Frozen at write time — never updated when the user record changes.
 *
 * {@code userId} is the only required field. {@code name} and {@code pictureUrl}
 * are nullable so a system actor (no human behind it) or a snapshot site that
 * never tracked a picture (e.g. {@link Reaction}, {@link AudienceSubmission})
 * can store {@code null}. {@code guest} captures whether the user was a guest
 * at snapshot time; {@code false} when the call site does not track it (e.g.
 * {@link DeckComment} requires a registered author).
 */
package cephadex.brainflex.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record UserSnapshot(
        String userId,
        String name,
        String pictureUrl,
        boolean guest) {

    public static UserSnapshot of(String userId, String name, String pictureUrl, boolean guest) {
        return new UserSnapshot(userId, name, pictureUrl, guest);
    }

    public static UserSnapshot of(String userId, String name, String pictureUrl) {
        return new UserSnapshot(userId, name, pictureUrl, false);
    }

    public static UserSnapshot of(String userId, String name) {
        return new UserSnapshot(userId, name, null, false);
    }

    public static UserSnapshot ofUser(User user) {
        if (user == null) return null;
        boolean isGuest = Boolean.TRUE.equals(user.getIsGuest());
        return new UserSnapshot(user.getId(), user.getUserName(), user.getPictureUrl(), isGuest);
    }
}
