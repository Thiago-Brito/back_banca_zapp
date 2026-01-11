package com.bancazapp.banca_zapp.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancazapp.banca_zapp.entity.VisitaItem;

public interface VisitaItemRepository extends JpaRepository<VisitaItem, UUID> {
}
