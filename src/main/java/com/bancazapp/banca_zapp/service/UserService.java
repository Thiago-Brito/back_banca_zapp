package com.bancazapp.banca_zapp.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.dto.UserRequestDto;
import com.bancazapp.banca_zapp.dto.UserResponseDto;
import com.bancazapp.banca_zapp.entity.User;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.mapper.UserMapper;
import com.bancazapp.banca_zapp.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDto criar(UserRequestDto dto) {
        User user = userMapper.toEntity(dto);
        user.setSenha(passwordEncoder.encode(dto.getSenha()));
        User salvo = userRepository.save(user);
        return userMapper.toResponse(salvo);
    }

    public List<UserResponseDto> listar() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponseDto buscarPorId(Long id) {
        return userMapper.toResponse(buscarEntidade(id));
    }

    public UserResponseDto atualizar(Long id, UserRequestDto dto) {
        User user = buscarEntidade(id);
        userMapper.updateEntity(dto, user);
        user.setSenha(passwordEncoder.encode(dto.getSenha()));
        User salvo = userRepository.save(user);
        return userMapper.toResponse(salvo);
    }

    public void deletar(Long id) {
        User user = buscarEntidade(id);
        userRepository.delete(user);
    }

    public User buscarPorLoginOuEmail(String loginOuEmail) {
        return userRepository.findByEmailIgnoreCase(loginOuEmail)
                .or(() -> userRepository.findByLoginIgnoreCase(loginOuEmail))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + loginOuEmail));
    }

    private User buscarEntidade(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + id));
    }
}
