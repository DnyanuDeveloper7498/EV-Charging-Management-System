package com.evcharging.repository;

import com.evcharging.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findAllByRole(String role);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRoleIn(Collection<String> roles);

    /** Row lock - used for every wallet balance change so concurrent requests cannot corrupt the balance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") int userId);
}
