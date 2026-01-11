package com.bancazapp.banca_zapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String tipo;
    private String token;
    private UserResponseDto usuario;
}
