package cephadex.brainflex.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cephadex.brainflex.dto.UserResponse;
import cephadex.brainflex.model.image.ImageSize;
import cephadex.brainflex.model.media.StoredImageVariant;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.ImageProcessingService;
import cephadex.brainflex.service.ImageProcessingService.ProcessedVariant;
import cephadex.brainflex.service.S3Service;
import cephadex.brainflex.service.UserImageHydrator;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/users")
public class AccountController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;
    private final UserImageHydrator userImageHydrator;

    public AccountController(
            UserRepository userRepository,
            UserService userService,
            S3Service s3Service,
            ImageProcessingService imageProcessingService,
            UserImageHydrator userImageHydrator) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.s3Service = s3Service;
        this.imageProcessingService = imageProcessingService;
        this.userImageHydrator = userImageHydrator;
    }

    // @PreAuthorize intentionally omitted: Spring Session + multipart has a
    // known compatibility issue where method-security advice sees an empty
    // SecurityContext and returns 401 even when the cookie/session are valid.
    // SecurityConfig.anyRequest().authenticated() still gates anonymous
    // callers; the inline resolveRegisteredUser check below upgrades that to
    // require a registered (non-guest) user.
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse.RegisteredUser> uploadProfileImage(
            @RequestParam("image") MultipartFile file,
            Authentication authentication) throws IOException {

        User user = userService.resolveRegisteredUser(authentication)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<ImageSize, ProcessedVariant> processed = imageProcessingService.processAvatar(file);
        List<StoredImageVariant> stored = s3Service.uploadAvatar(user.getId(), processed);

        // Uploaded variants supersede the OAuth picture URL. Clear it so the
        // hydrator's preference order (variants > pictureUrl > empty) keeps
        // pointing at the user's own image even after a re-sign-in.
        user.setPictureVariants(stored);
        user.setPictureUrl(null);
        userRepository.save(user);

        return ResponseEntity.ok(new UserResponse.RegisteredUser(user, userImageHydrator.pictureImageOf(user)));
    }
}
