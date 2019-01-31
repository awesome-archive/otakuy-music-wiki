package com.otakuy.otakuymusic.repository;

import com.otakuy.otakuymusic.model.User;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

    Mono<User> findByUsername(String username);

    Mono<User> findByEmail(String email);

    @Query("{$or: [ { 'username': ?0 }, { 'email': ?1  } ]}")
    Flux<User> findByUsernameOrEmail(String username, String email);

}
