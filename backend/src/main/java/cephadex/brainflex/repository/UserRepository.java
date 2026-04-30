package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    @Override
    Page<User> findAll(Pageable pageable);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByUserName(String userName);

}