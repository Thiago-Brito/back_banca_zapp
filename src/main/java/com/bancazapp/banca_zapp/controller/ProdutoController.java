package com.bancazapp.banca_zapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bancazapp.banca_zapp.dto.ProdutoDto;
import com.bancazapp.banca_zapp.service.ProdutoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/produtos")
@Validated
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<ProdutoDto> criar(@Valid @RequestBody ProdutoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<ProdutoDto>> listar() {
        return ResponseEntity.ok(produtoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDto> atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoDto dto) {
        return ResponseEntity.ok(produtoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
