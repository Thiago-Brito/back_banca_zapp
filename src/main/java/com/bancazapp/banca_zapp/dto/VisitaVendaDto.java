package com.bancazapp.banca_zapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VisitaVendaDto {

    private BigDecimal valorTotal;
    private BigDecimal valorPago;
    private boolean pago;
    private LocalDate dataPagamento;
}
