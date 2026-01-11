package com.bancazapp.banca_zapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bancazapp.banca_zapp.dto.VisitaDto;
import com.bancazapp.banca_zapp.service.VisitaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/visitas")
@Validated
public class VisitaController {

    private final VisitaService visitaService;

    public VisitaController(VisitaService visitaService) {
        this.visitaService = visitaService;
    }

    @PostMapping
    public ResponseEntity<VisitaDto> registrar(@Valid @RequestBody VisitaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitaService.registrarVisita(dto));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VisitaDto>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(visitaService.listarPorCliente(clienteId));
    }
}
