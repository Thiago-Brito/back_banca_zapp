package com.bancazapp.banca_zapp.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "visita_itens")
public class VisitaItem {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "visita_id", nullable = false)
    private Visita visita;

    @Column(nullable = false)
    private Integer possuia;

    @Column(nullable = false)
    private Integer entregue;

    @Column(nullable = false)
    private Integer vendido;

    @Column(nullable = false)
    private Integer retirado;

    @Column(nullable = false)
    private Integer possuiAgora;
}
