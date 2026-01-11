package com.bancazapp.banca_zapp.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.VisitaDto;
import com.bancazapp.banca_zapp.dto.VisitaItemDto;
import com.bancazapp.banca_zapp.entity.Visita;

@Component
public class VisitaMapper {

    private final VisitaItemMapper visitaItemMapper;

    public VisitaMapper(VisitaItemMapper visitaItemMapper) {
        this.visitaItemMapper = visitaItemMapper;
    }

    public VisitaDto toDto(Visita entity) {
        if (entity == null) {
            return null;
        }
        Long clienteId = entity.getCliente() != null ? entity.getCliente().getId() : null;
        List<VisitaItemDto> itens = entity.getItens().stream()
                .map(visitaItemMapper::toDto)
                .toList();
        return new VisitaDto(
                entity.getId(),
                clienteId,
                entity.getDataVisita(),
                entity.getObservacoes(),
                itens
        );
    }
}
