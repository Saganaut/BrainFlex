package cephadex.brainflex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Organization;

public interface OrganizationRepository extends MongoRepository<Organization, String> {

    Optional<Organization> findByOwnerId(String ownerId);

    /** Indexed on {@code emailDomain}; case-sensitive by storage convention
     *  (we lowercase on write and on lookup). */
    List<Organization> findByEmailDomain(String emailDomain);

    /** Indexed on {@code inviteCode}; the code is opaque so the match is exact. */
    Optional<Organization> findByInviteCode(String inviteCode);
}
