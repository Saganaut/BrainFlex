/**
 * Business logic for browsing and retrieving content packs.
 * Keeps pack queries out of the controller and provides a central place
 * to add filtering, pagination, or access control as the pack system grows.
 */
package cephadex.brainflex.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.model.ContentPack;
import cephadex.brainflex.repository.ContentPackRepository;

@Service
public class ContentPackService {

    private final ContentPackRepository contentPackRepository;

    public ContentPackService(ContentPackRepository contentPackRepository) {
        this.contentPackRepository = contentPackRepository;
    }

    public List<ContentPack> listPublic() {
        return contentPackRepository.findByIsPublicTrue();
    }

    public ContentPack getById(String id) {
        return contentPackRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content pack not found"));
    }
}
