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

import com.bancazapp.banca_zapp.dto.CategoriaDto;
import com.bancazapp.banca_zapp.service.CategoriaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/categorias")
@Validated
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<CategoriaDto> criar(@Valid @RequestBody CategoriaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDto>> listar() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDto> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaDto dto) {
        return ResponseEntity.ok(categoriaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        categoriaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
