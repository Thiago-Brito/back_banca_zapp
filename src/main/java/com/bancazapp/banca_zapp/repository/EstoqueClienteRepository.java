package com.bancazapp.banca_zapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancazapp.banca_zapp.entity.EstoqueCliente;

public interface EstoqueClienteRepository extends JpaRepository<EstoqueCliente, Long> {

    List<EstoqueCliente> findByClienteId(Long clienteId);

    Optional<EstoqueCliente> findByClienteIdAndProdutoId(Long clienteId, Long produtoId);
}
