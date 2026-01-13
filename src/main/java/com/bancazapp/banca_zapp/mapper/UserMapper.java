package com.bancazapp.banca_zapp.mapper;

import org.springframework.stereotype.Component;

import com.bancazapp.banca_zapp.dto.UserRequestDto;
import com.bancazapp.banca_zapp.dto.UserResponseDto;
import com.bancazapp.banca_zapp.entity.User;

@Component
public class UserMapper {

    public UserResponseDto toResponse(User entity) {
        if (entity == null) {
            return null;
        }
        return new UserResponseDto(
                entity.getId(),
                entity.getNome(),
                entity.getNomeFantasia(),
                entity.getTelefone(),
                entity.getEmail(),
                entity.getLogin(),
                entity.getCnpj()
        );
    }

    public User toEntity(UserRequestDto dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setId(dto.getId());
        user.setNome(dto.getNome());
        user.setNomeFantasia(dto.getNomeFantasia());
        user.setTelefone(dto.getTelefone());
        user.setEmail(dto.getEmail());
        user.setLogin(dto.getLogin());
        user.setSenha(dto.getSenha());
        user.setCnpj(dto.getCnpj());
        return user;
    }

    public void updateEntity(UserRequestDto dto, User entity) {
        entity.setNome(dto.getNome());
        entity.setNomeFantasia(dto.getNomeFantasia());
        entity.setTelefone(dto.getTelefone());
        entity.setEmail(dto.getEmail());
        entity.setLogin(dto.getLogin());
        entity.setSenha(dto.getSenha());
        entity.setCnpj(dto.getCnpj());
    }
}
