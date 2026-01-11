package com.bancazapp.banca_zapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.dto.LocalidadeDto;
import com.bancazapp.banca_zapp.entity.Localidade;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.mapper.LocalidadeMapper;
import com.bancazapp.banca_zapp.repository.LocalidadeRepository;

@Service
public class LocalidadeService {

    private final LocalidadeRepository localidadeRepository;
    private final LocalidadeMapper localidadeMapper;

    public LocalidadeService(LocalidadeRepository localidadeRepository, LocalidadeMapper localidadeMapper) {
        this.localidadeRepository = localidadeRepository;
        this.localidadeMapper = localidadeMapper;
    }

    public LocalidadeDto criar(LocalidadeDto dto) {
        Localidade localidade = localidadeMapper.toEntity(dto);
        Localidade salva = localidadeRepository.save(localidade);
        return localidadeMapper.toDto(salva);
    }

    public List<LocalidadeDto> listar() {
        return localidadeRepository.findAll().stream()
                .map(localidadeMapper::toDto)
                .toList();
    }

    public LocalidadeDto buscarPorId(Long id) {
        return localidadeMapper.toDto(buscarEntidade(id));
    }

    public LocalidadeDto atualizar(Long id, LocalidadeDto dto) {
        Localidade localidade = buscarEntidade(id);
        localidadeMapper.updateEntity(dto, localidade);
        Localidade salva = localidadeRepository.save(localidade);
        return localidadeMapper.toDto(salva);
    }

    public void deletar(Long id) {
        Localidade localidade = buscarEntidade(id);
        localidadeRepository.delete(localidade);
    }

    private Localidade buscarEntidade(Long id) {
        return localidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localidade nao encontrada: " + id));
    }
}
