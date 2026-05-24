/**
 * Unified avatar for a session player — one value object that replaces the old
 * standalone {@code avatarKey} field living alongside the user's real
 * {@code pictureUrl}. Consumers read the single {@code avatarType} discriminator
 * and render by case instead of repeating a "preset-or-pictureUrl" branch.
 *
 *   KEY  — {@code avatarKey} is a preset id (e.g. {@code "fox-orange"}); the
 *          frontend resolves it to a bundled image. {@code avatarUrl} is null.
 *   LINK — {@code avatarUrl} is a direct image URL (the player's real picture);
 *          {@code avatarKey} is null. {@code avatarUrl} may itself be null for a
 *          guest / OAuth user with no picture, in which case the frontend falls
 *          back to an initial/icon.
 *
 * Stored on the player as an intent marker: for LINK the {@code avatarUrl} is
 * left null in the document and materialized at the DTO boundary from the
 * (re-hydrated) {@code UserSnapshot.pictureUrl}, so a long-lived session never
 * freezes an expiring presigned S3 URL. See {@code InteractiveSessionPlayer}.
 *
 * Always use the {@link #ofKey} / {@link #ofLink} factories so the
 * KEY⇒key-only / LINK⇒url-only invariant is enforced at the single
 * construction point.
 */
package cephadex.brainflex.model.shared;

import com.fasterxml.jackson.annotation.JsonInclude;

import cephadex.brainflex.model.enums.AvatarType;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record Avatar(AvatarType avatarType, String avatarUrl, String avatarKey) {

    public static Avatar ofKey(String avatarKey) {
        return new Avatar(AvatarType.KEY, null, avatarKey);
    }

    public static Avatar ofLink(String avatarUrl) {
        return new Avatar(AvatarType.LINK, avatarUrl, null);
    }
}
