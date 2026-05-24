/**
 * Discriminates the two ways a session player's avatar can be expressed, so a
 * single {@link cephadex.brainflex.model.shared.Avatar} field replaces the old
 * "preset key OR real pictureUrl" branch every consumer used to repeat.
 *
 *   KEY  — the player picked a Kahoot-style preset; the avatar carries an
 *          {@code avatarKey} the frontend resolves to a bundled image.
 *   LINK — the avatar is a direct image URL (the player's real pictureUrl, or
 *          null when neither a preset nor a picture is available — guests).
 */
package cephadex.brainflex.model.enums;

public enum AvatarType {
    KEY,
    LINK
}
