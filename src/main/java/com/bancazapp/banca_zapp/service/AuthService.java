package com.bancazapp.banca_zapp.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.dto.LoginRequestDto;
import com.bancazapp.banca_zapp.dto.LoginResponseDto;
import com.bancazapp.banca_zapp.entity.User;
import com.bancazapp.banca_zapp.mapper.UserMapper;
import com.bancazapp.banca_zapp.security.JwtService;

@Service
public class AuthService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserService userService,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDto login(LoginRequestDto dto) {
        User user = userService.buscarPorLoginOuEmail(dto.getLoginOuEmail());
        if (!passwordEncoder.matches(dto.getSenha(), user.getSenha())) {
            throw new IllegalArgumentException("Credenciais invalidas.");
        }
        String token = jwtService.generateToken(user);
        return new LoginResponseDto("Bearer", token, userMapper.toResponse(user));
    }
}
