package com.bancazapp.banca_zapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancazapp.banca_zapp.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByLoginIgnoreCase(String login);
}
