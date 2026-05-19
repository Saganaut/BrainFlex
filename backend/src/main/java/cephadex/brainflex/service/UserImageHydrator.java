/**
 * Builds a wire-ready {@link Image} for a user's avatar. Uploaded variants
 * take precedence over the OAuth {@code pictureUrl}; if neither is present,
 * returns {@link Image#empty()}.
 *
 * Lives outside the DTOs because the DTOs are bare records and shouldn't
 * carry a service dependency. Every caller that hands a {@link User} to a
 * UserDTO constructor first resolves the picture through this hydrator.
 */
package cephadex.brainflex.service;

import org.springframework.stereotype.Service;

import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.Image;
import cephadex.brainflex.model.element.ImageVariant;

@Service
public class UserImageHydrator {

    private final S3Service s3Service;

    public UserImageHydrator(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /** Resolves the user's avatar to an Image at every {@link cephadex.brainflex.model.element.ImageSize}
     *  tier that exists for them. Read-only — does not mutate the user. */
    public Image pictureImageOf(User user) {
        if (user == null) return Image.empty();
        if (user.getPictureVariants() != null && !user.getPictureVariants().isEmpty()) {
            return new Image(false, null, s3Service.refreshAvatar(user.getId(), user.getPictureVariants()));
        }
        String external = user.getPictureUrl();
        if (external != null && !external.isBlank()) {
            return Image.external(external);
        }
        return Image.empty();
    }

    /** Largest URL we can produce for the user at this moment. Used by call
     *  sites that store a single avatar URL inline (comments, showcase
     *  players, ratings, …). For uploaded avatars this is a fresh presigned
     *  URL to the xl rendition; for OAuth users it's the externally-hosted
     *  Google URL. Null when neither source exists. */
    public String pictureUrlOf(User user) {
        Image picture = pictureImageOf(user);
        if (picture == null) return null;
        ImageVariant largest = picture.largestVariant();
        return largest == null ? null : largest.url();
    }
}

