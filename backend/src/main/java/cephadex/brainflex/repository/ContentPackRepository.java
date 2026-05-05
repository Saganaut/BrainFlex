/**
 * MongoDB repository for ContentPack documents.
 * Exposes queries for public packs and system-seeded packs so the
 * game creation UI can display available content to users.
 */
package cephadex.brainflex.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.ContentPack;

public interface ContentPackRepository extends MongoRepository<ContentPack, String> {

    List<ContentPack> findByIsPublicTrue();

    List<ContentPack> findByIsSystemTrue();

    List<ContentPack> findByCreatorUserId(String creatorUserId);
}
