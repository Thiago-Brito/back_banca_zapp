package com.bancazapp.banca_zapp.mapper;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.LocalidadeDto;
import com.bancazapp.banca_zapp.entity.Localidade;

@Component
public class LocalidadeMapper {

    public LocalidadeDto toDto(Localidade entity) {
        if (entity == null) {
            return null;
        }
        return new LocalidadeDto(entity.getId(), entity.getNome());
    }

    public Localidade toEntity(LocalidadeDto dto) {
        if (dto == null) {
            return null;
        }
        Localidade localidade = new Localidade();
        localidade.setId(dto.getId());
        localidade.setNome(dto.getNome());
        return localidade;
    }

    public void updateEntity(LocalidadeDto dto, Localidade entity) {
        entity.setNome(dto.getNome());
    }
}
