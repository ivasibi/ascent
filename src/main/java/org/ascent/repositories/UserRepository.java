package org.ascent.repositories;

import org.ascent.entities.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @Caching(put = {
            @CachePut(value = ":users:email", key = "#entity.email")
    })
    <S extends User> S save(S entity);

    @Override
    @Caching(evict = {
            @CacheEvict(value = ":users:email", key = "#entity.email")
    })
    void delete(User entity);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Cacheable(value = ":users:email", key = "#email", unless = "#result == null")
    User findByEmail(String email);
}