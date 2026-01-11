package com.bancazapp.banca_zapp.dto;

import java.util.UUID;

import com.bancazapp.banca_zapp.entity.TipoVisita;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VisitaItemDto {

    private UUID id;

    @NotNull
    private Long produtoId;

    private TipoVisita tipo;

    @NotNull
    @PositiveOrZero
    private Integer possuia;

    @NotNull
    @PositiveOrZero
    private Integer entregue;

    @NotNull
    @PositiveOrZero
    private Integer vendido;

    @NotNull
    @PositiveOrZero
    private Integer retirado;

    private Integer possuiAgora;
}
