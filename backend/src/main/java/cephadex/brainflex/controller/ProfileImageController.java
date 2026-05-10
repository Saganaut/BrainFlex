package cephadex.brainflex.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cephadex.brainflex.dto.UserDTO;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.ImageProcessingService;
import cephadex.brainflex.service.S3Service;
import cephadex.brainflex.service.UserService;

@RestController
@RequestMapping("/api/users")
public class ProfileImageController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;

    public ProfileImageController(
            UserRepository userRepository,
            UserService userService,
            S3Service s3Service,
            ImageProcessingService imageProcessingService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.s3Service = s3Service;
        this.imageProcessingService = imageProcessingService;
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO.RegisteredUser> uploadProfileImage(
            @RequestParam("image") MultipartFile file,
            Authentication authentication) throws IOException {

        User user = userService.resolveRegisteredUser(authentication)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] processed = imageProcessingService.validateAndProcess(file);
        String presignedUrl = s3Service.uploadProfileImage(user.getId(), processed);

        user.setPictureUrl(presignedUrl);
        userRepository.save(user);

        return ResponseEntity.ok(new UserDTO.RegisteredUser(user));
    }
}
