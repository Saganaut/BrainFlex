/**
 * Read-only endpoint serving the static preset avatar pool the interactiveSession
 * lobby picker reads on mount.
 *
 * Chunk 13 — the avatar pool is currently a compile-time constant
 * ({@link AvatarService}); the controller exists so the frontend can
 * discover it without bundling the list a second time, and so the codegen
 * picks it up as an RTK Query hook.
 */
package cephadex.brainflex.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.service.AvatarService;
import cephadex.brainflex.service.AvatarService.AvatarPreset;

@RestController
@RequestMapping("/api/avatars")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @GetMapping
    public List<AvatarPreset> list() {
        return avatarService.list();
    }
}
