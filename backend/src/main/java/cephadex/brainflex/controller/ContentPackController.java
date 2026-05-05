/**
 * REST endpoints for browsing available content packs.
 * Both endpoints are public so the game creation UI can load packs
 * without requiring the user to be logged in.
 */
package cephadex.brainflex.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cephadex.brainflex.dto.ContentPackDTO;
import cephadex.brainflex.service.ContentPackService;

@RestController
@RequestMapping("/api/content-packs")
public class ContentPackController {

    private final ContentPackService contentPackService;

    public ContentPackController(ContentPackService contentPackService) {
        this.contentPackService = contentPackService;
    }

    @GetMapping
    public List<ContentPackDTO> listPacks() {
        return contentPackService.listPublic().stream()
                .map(ContentPackDTO::new)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentPackDTO> getPack(@PathVariable String id) {
        return ResponseEntity.ok(new ContentPackDTO(contentPackService.getById(id)));
    }
}
