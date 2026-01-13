package com.bancazapp.banca_zapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "usuarios")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(length = 160)
    private String nomeFantasia;

    @Column(length = 40)
    private String telefone;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(nullable = false, length = 80, unique = true)
    private String login;

    @Column(nullable = false, length = 120)
    private String senha;

    @Column(nullable = false, length = 18, unique = true)
    private String cnpj;
}
