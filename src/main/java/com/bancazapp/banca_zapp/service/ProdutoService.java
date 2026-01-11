package com.bancazapp.banca_zapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.dto.ProdutoDto;
import com.bancazapp.banca_zapp.entity.Categoria;
import com.bancazapp.banca_zapp.entity.Produto;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.mapper.ProdutoMapper;
import com.bancazapp.banca_zapp.repository.CategoriaRepository;
import com.bancazapp.banca_zapp.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoMapper produtoMapper;

    public ProdutoService(ProdutoRepository produtoRepository,
                          CategoriaRepository categoriaRepository,
                          ProdutoMapper produtoMapper) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoMapper = produtoMapper;
    }

    public ProdutoDto criar(ProdutoDto dto) {
        Categoria categoria = buscarCategoria(dto.getCategoriaId());
        Produto produto = produtoMapper.toEntity(dto);
        produto.setCategoria(categoria);
        Produto salvo = produtoRepository.save(produto);
        return produtoMapper.toDto(salvo);
    }

    public List<ProdutoDto> listar() {
        return produtoRepository.findAll().stream()
                .map(produtoMapper::toDto)
                .toList();
    }

    public ProdutoDto buscarPorId(Long id) {
        return produtoMapper.toDto(buscarEntidade(id));
    }

    public ProdutoDto atualizar(Long id, ProdutoDto dto) {
        Produto produto = buscarEntidade(id);
        Categoria categoria = buscarCategoria(dto.getCategoriaId());
        produtoMapper.updateEntity(dto, produto);
        produto.setCategoria(categoria);
        Produto salvo = produtoRepository.save(produto);
        return produtoMapper.toDto(salvo);
    }

    public void deletar(Long id) {
        Produto produto = buscarEntidade(id);
        produtoRepository.delete(produto);
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado: " + id));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada: " + id));
    }
}
