package com.bancazapp.banca_zapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.dto.CategoriaDto;
import com.bancazapp.banca_zapp.entity.Categoria;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.mapper.CategoriaMapper;
import com.bancazapp.banca_zapp.repository.CategoriaRepository;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    public CategoriaService(CategoriaRepository categoriaRepository, CategoriaMapper categoriaMapper) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
    }

    public CategoriaDto criar(CategoriaDto dto) {
        Categoria categoria = categoriaMapper.toEntity(dto);
        Categoria salva = categoriaRepository.save(categoria);
        return categoriaMapper.toDto(salva);
    }

    public List<CategoriaDto> listar() {
        return categoriaRepository.findAll().stream()
                .map(categoriaMapper::toDto)
                .toList();
    }

    public CategoriaDto buscarPorId(Long id) {
        return categoriaMapper.toDto(buscarEntidade(id));
    }

    public CategoriaDto atualizar(Long id, CategoriaDto dto) {
        Categoria categoria = buscarEntidade(id);
        categoriaMapper.updateEntity(dto, categoria);
        Categoria salva = categoriaRepository.save(categoria);
        return categoriaMapper.toDto(salva);
    }

    public void deletar(Long id) {
        Categoria categoria = buscarEntidade(id);
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada: " + id));
    }
}
