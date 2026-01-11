package com.bancazapp.banca_zapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bancazapp.banca_zapp.dto.EstoqueClienteDto;
import com.bancazapp.banca_zapp.service.EstoqueClienteService;

@RestController
@RequestMapping("/api/v1/estoques")
@Validated
public class EstoqueClienteController {

    private final EstoqueClienteService estoqueClienteService;

    public EstoqueClienteController(EstoqueClienteService estoqueClienteService) {
        this.estoqueClienteService = estoqueClienteService;
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<EstoqueClienteDto>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(estoqueClienteService.listarPorCliente(clienteId));
    }
}
