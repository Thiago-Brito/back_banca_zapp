package com.bancazapp.banca_zapp.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(nullable = false, length = 40)
    private String telefone;

    @Column(nullable = false, length = 240)
    private String endereco;

    @ManyToOne(optional = false)
    @JoinColumn(name = "localidade_id", nullable = false)
    private Localidade localidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal comissao;
}
