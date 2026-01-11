package com.bancazapp.banca_zapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDto {

    private Long id;

    @NotBlank
    private String nome;

    @NotNull
    @Positive
    private BigDecimal preco;

    @NotNull
    private Long categoriaId;
}
