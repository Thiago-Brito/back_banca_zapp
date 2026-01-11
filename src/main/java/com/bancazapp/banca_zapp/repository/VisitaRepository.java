package com.bancazapp.banca_zapp.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.bancazapp.banca_zapp.entity.Visita;

public interface VisitaRepository extends JpaRepository<Visita, UUID> {

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    List<Visita> findByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    Optional<Visita> findWithItensById(UUID id);
}
