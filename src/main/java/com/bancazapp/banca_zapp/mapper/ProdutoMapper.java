package com.bancazapp.banca_zapp.mapper;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.ProdutoDto;
import com.bancazapp.banca_zapp.entity.Produto;

@Component
public class ProdutoMapper {

    public ProdutoDto toDto(Produto entity) {
        if (entity == null) {
            return null;
        }
        Long categoriaId = entity.getCategoria() != null ? entity.getCategoria().getId() : null;
        return new ProdutoDto(entity.getId(), entity.getNome(), entity.getPreco(), categoriaId);
    }

    public Produto toEntity(ProdutoDto dto) {
        if (dto == null) {
            return null;
        }
        Produto produto = new Produto();
        produto.setId(dto.getId());
        produto.setNome(dto.getNome());
        produto.setPreco(dto.getPreco());
        return produto;
    }

    public void updateEntity(ProdutoDto dto, Produto entity) {
        entity.setNome(dto.getNome());
        entity.setPreco(dto.getPreco());
    }
}
