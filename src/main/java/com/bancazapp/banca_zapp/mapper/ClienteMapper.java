package com.bancazapp.banca_zapp.mapper;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.ClienteDto;
import com.bancazapp.banca_zapp.entity.Cliente;

@Component
public class ClienteMapper {

    public ClienteDto toDto(Cliente entity) {
        if (entity == null) {
            return null;
        }
        Long localidadeId = entity.getLocalidade() != null ? entity.getLocalidade().getId() : null;
        return new ClienteDto(
                entity.getId(),
                entity.getNome(),
                entity.getEmail(),
                entity.getTelefone(),
                entity.getEndereco(),
                localidadeId,
                entity.getComissao()
        );
    }

    public Cliente toEntity(ClienteDto dto) {
        if (dto == null) {
            return null;
        }
        Cliente cliente = new Cliente();
        cliente.setId(dto.getId());
        cliente.setNome(dto.getNome());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setEndereco(dto.getEndereco());
        cliente.setComissao(dto.getComissao());
        return cliente;
    }

    public void updateEntity(ClienteDto dto, Cliente entity) {
        entity.setNome(dto.getNome());
        entity.setEmail(dto.getEmail());
        entity.setTelefone(dto.getTelefone());
        entity.setEndereco(dto.getEndereco());
        entity.setComissao(dto.getComissao());
    }
}
