package com.bancazapp.banca_zapp.mapper;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.CategoriaDto;
import com.bancazapp.banca_zapp.entity.Categoria;

@Component
public class CategoriaMapper {

    public CategoriaDto toDto(Categoria entity) {
        if (entity == null) {
            return null;
        }
        return new CategoriaDto(entity.getId(), entity.getNome());
    }

    public Categoria toEntity(CategoriaDto dto) {
        if (dto == null) {
            return null;
        }
        Categoria categoria = new Categoria();
        categoria.setId(dto.getId());
        categoria.setNome(dto.getNome());
        return categoria;
    }

    public void updateEntity(CategoriaDto dto, Categoria entity) {
        entity.setNome(dto.getNome());
    }
}
