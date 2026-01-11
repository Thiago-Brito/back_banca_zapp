package com.bancazapp.banca_zapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bancazapp.banca_zapp.dto.ClienteDto;
import com.bancazapp.banca_zapp.entity.Cliente;
import com.bancazapp.banca_zapp.entity.Localidade;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.mapper.ClienteMapper;
import com.bancazapp.banca_zapp.repository.ClienteRepository;
import com.bancazapp.banca_zapp.repository.LocalidadeRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final LocalidadeRepository localidadeRepository;
    private final ClienteMapper clienteMapper;

    public ClienteService(ClienteRepository clienteRepository,
                          LocalidadeRepository localidadeRepository,
                          ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.localidadeRepository = localidadeRepository;
        this.clienteMapper = clienteMapper;
    }

    public ClienteDto criar(ClienteDto dto) {
        Localidade localidade = buscarLocalidade(dto.getLocalidadeId());
        Cliente cliente = clienteMapper.toEntity(dto);
        cliente.setLocalidade(localidade);
        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.toDto(salvo);
    }

    public List<ClienteDto> listar() {
        return clienteRepository.findAll().stream()
                .map(clienteMapper::toDto)
                .toList();
    }

    public ClienteDto buscarPorId(Long id) {
        return clienteMapper.toDto(buscarEntidade(id));
    }

    public ClienteDto atualizar(Long id, ClienteDto dto) {
        Cliente cliente = buscarEntidade(id);
        Localidade localidade = buscarLocalidade(dto.getLocalidadeId());
        clienteMapper.updateEntity(dto, cliente);
        cliente.setLocalidade(localidade);
        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.toDto(salvo);
    }

    public void deletar(Long id) {
        Cliente cliente = buscarEntidade(id);
        clienteRepository.delete(cliente);
    }

    private Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + id));
    }

    private Localidade buscarLocalidade(Long id) {
        return localidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localidade nao encontrada: " + id));
    }
}
