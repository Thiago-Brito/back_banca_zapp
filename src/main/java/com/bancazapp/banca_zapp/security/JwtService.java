package com.bancazapp.banca_zapp.security;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.config.JwtProperties;
import com.bancazapp.banca_zapp.entity.User;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(User user) {
        // Estrutura preparada para assinar JWT futuramente com issuer/expiracao.
        return jwtProperties.getIssuer() + "-" + UUID.randomUUID();
    }
}
