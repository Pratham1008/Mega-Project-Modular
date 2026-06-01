package com.megaproject.auth.repository;

import com.megaproject.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Collection<User> findAllByEmailIn(Set<String> emails);

    @Query(value = "{}", fields = "{ 'email' : 1, '_id' : 0 }")
    Collection<String> findAllEmails();
}
