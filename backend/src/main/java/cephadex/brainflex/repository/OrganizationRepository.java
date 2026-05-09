package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Organization;

public interface OrganizationRepository extends MongoRepository<Organization, String> {

    Optional<Organization> findByOwnerId(String ownerId);
}
