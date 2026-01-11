package com.bancazapp.banca_zapp.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bancazapp.banca_zapp.dto.VisitaDto;
import com.bancazapp.banca_zapp.service.NotaConferenciaService;
import com.bancazapp.banca_zapp.service.VisitaService;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visitas")
@Validated
public class VisitaController {

    private final VisitaService visitaService;
    private final NotaConferenciaService notaConferenciaService;

    public VisitaController(VisitaService visitaService,
                            NotaConferenciaService notaConferenciaService) {
        this.visitaService = visitaService;
        this.notaConferenciaService = notaConferenciaService;
    }

    @PostMapping
    public ResponseEntity<VisitaDto> registrar(@Valid @RequestBody VisitaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitaService.registrarVisita(dto));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VisitaDto>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(visitaService.listarPorCliente(clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisitaDto> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(visitaService.buscarPorId(id));
    }

    @GetMapping("/{id}/nota-conferencia")
    public ResponseEntity<byte[]> gerarNotaConferencia(@PathVariable UUID id,
                                                       @RequestParam(name = "formato", defaultValue = "a4") String formato) {
        byte[] pdf = notaConferenciaService.gerarNotaConferencia(id, formato);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("nota-conferencia-" + id + ".pdf")
                .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
