package com.bancazapp.banca_zapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.dto.EstoqueClienteDto;
import com.bancazapp.banca_zapp.mapper.EstoqueClienteMapper;
import com.bancazapp.banca_zapp.repository.EstoqueClienteRepository;

@Service
public class EstoqueClienteService {

    private final EstoqueClienteRepository estoqueClienteRepository;
    private final EstoqueClienteMapper estoqueClienteMapper;

    public EstoqueClienteService(EstoqueClienteRepository estoqueClienteRepository,
                                 EstoqueClienteMapper estoqueClienteMapper) {
        this.estoqueClienteRepository = estoqueClienteRepository;
        this.estoqueClienteMapper = estoqueClienteMapper;
    }

    public List<EstoqueClienteDto> listarPorCliente(Long clienteId) {
        return estoqueClienteRepository.findByClienteId(clienteId).stream()
                .map(estoqueClienteMapper::toDto)
                .toList();
    }
}
