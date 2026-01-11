package com.bancazapp.banca_zapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancazapp.banca_zapp.entity.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
