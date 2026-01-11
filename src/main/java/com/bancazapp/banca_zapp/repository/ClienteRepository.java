package com.bancazapp.banca_zapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancazapp.banca_zapp.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);
}
