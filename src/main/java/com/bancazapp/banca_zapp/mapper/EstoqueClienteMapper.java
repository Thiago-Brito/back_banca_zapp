package com.bancazapp.banca_zapp.mapper;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.EstoqueClienteDto;
import com.bancazapp.banca_zapp.entity.EstoqueCliente;

@Component
public class EstoqueClienteMapper {

    public EstoqueClienteDto toDto(EstoqueCliente entity) {
        if (entity == null) {
            return null;
        }
        Long clienteId = entity.getCliente() != null ? entity.getCliente().getId() : null;
        Long produtoId = entity.getProduto() != null ? entity.getProduto().getId() : null;
        return new EstoqueClienteDto(entity.getId(), clienteId, produtoId, entity.getQuantidade());
    }
}
