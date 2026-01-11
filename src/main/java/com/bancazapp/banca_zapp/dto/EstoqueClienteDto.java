package com.bancazapp.banca_zapp.dto;

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
public class EstoqueClienteDto {

    private Long id;

    @NotNull
    private Long clienteId;

    @NotNull
    private Long produtoId;

    @NotNull
    @PositiveOrZero
    private Integer quantidade;
}
