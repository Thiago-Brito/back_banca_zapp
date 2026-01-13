package com.bancazapp.banca_zapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String nome;
    private String nomeFantasia;
    private String telefone;
    private String email;
    private String login;
    private String cnpj;
}
