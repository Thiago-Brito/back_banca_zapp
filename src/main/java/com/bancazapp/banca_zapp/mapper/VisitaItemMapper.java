package com.bancazapp.banca_zapp.mapper;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.VisitaItemDto;
import com.bancazapp.banca_zapp.entity.TipoVisita;
import com.bancazapp.banca_zapp.entity.VisitaItem;

@Component
public class VisitaItemMapper {

    public VisitaItemDto toDto(VisitaItem entity, TipoVisita tipo) {
        if (entity == null) {
            return null;
        }
        Long produtoId = entity.getProduto() != null ? entity.getProduto().getId() : null;
        return new VisitaItemDto(
                entity.getId(),
                produtoId,
                tipo,
                entity.getPossuia(),
                entity.getEntregue(),
                entity.getVendido(),
                entity.getRetirado(),
                entity.getPossuiAgora()
        );
    }

    public void updateEntity(VisitaItemDto dto, VisitaItem entity) {
        entity.setPossuia(dto.getPossuia());
        entity.setEntregue(dto.getEntregue());
        entity.setVendido(dto.getVendido());
        entity.setRetirado(dto.getRetirado());
        entity.setPossuiAgora(dto.getPossuiAgora());
    }
}
