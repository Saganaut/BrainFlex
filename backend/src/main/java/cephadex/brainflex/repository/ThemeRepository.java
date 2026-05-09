package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Theme;

public interface ThemeRepository extends MongoRepository<Theme, String> {

    List<Theme> findByOwnerId(String ownerId);

    List<Theme> findByOrganizationId(String organizationId);

    boolean existsByIdAndOwnerId(String id, String ownerId);
}
