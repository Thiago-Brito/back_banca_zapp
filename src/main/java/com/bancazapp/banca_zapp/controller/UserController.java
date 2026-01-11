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

import com.bancazapp.banca_zapp.dto.UserRequestDto;
import com.bancazapp.banca_zapp.dto.UserResponseDto;
import com.bancazapp.banca_zapp.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> criar(@Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> listar() {
        return ResponseEntity.ok(userService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> atualizar(@PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        userService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
