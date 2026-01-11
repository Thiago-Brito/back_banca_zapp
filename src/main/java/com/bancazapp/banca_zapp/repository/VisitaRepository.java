package com.bancazapp.banca_zapp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancazapp.banca_zapp.entity.Visita;

public interface VisitaRepository extends JpaRepository<Visita, UUID> {

    List<Visita> findByClienteId(Long clienteId);
}
