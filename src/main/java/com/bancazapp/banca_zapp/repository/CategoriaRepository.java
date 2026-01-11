package com.bancazapp.banca_zapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancazapp.banca_zapp.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
