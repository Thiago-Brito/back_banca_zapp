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

import com.bancazapp.banca_zapp.dto.LocalidadeDto;
import com.bancazapp.banca_zapp.service.LocalidadeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/localidades")
@Validated
public class LocalidadeController {

    private final LocalidadeService localidadeService;

    public LocalidadeController(LocalidadeService localidadeService) {
        this.localidadeService = localidadeService;
    }

    @PostMapping
    public ResponseEntity<LocalidadeDto> criar(@Valid @RequestBody LocalidadeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(localidadeService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<LocalidadeDto>> listar() {
        return ResponseEntity.ok(localidadeService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocalidadeDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(localidadeService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocalidadeDto> atualizar(@PathVariable Long id, @Valid @RequestBody LocalidadeDto dto) {
        return ResponseEntity.ok(localidadeService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        localidadeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
