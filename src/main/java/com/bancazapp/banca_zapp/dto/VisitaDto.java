package com.bancazapp.banca_zapp.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VisitaDto {

    private UUID id;

    @NotNull
    private Long clienteId;

    @NotNull
    private LocalDate dataVisita;

    private String observacoes;

    @Valid
    @NotEmpty
    private List<VisitaItemDto> itens = new ArrayList<>();
}
