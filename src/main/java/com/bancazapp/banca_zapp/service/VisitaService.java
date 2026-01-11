package com.bancazapp.banca_zapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bancazapp.banca_zapp.dto.VisitaDto;
import com.bancazapp.banca_zapp.dto.VisitaItemDto;
import com.bancazapp.banca_zapp.entity.Cliente;
import com.bancazapp.banca_zapp.entity.EstoqueCliente;
import com.bancazapp.banca_zapp.entity.Produto;
import com.bancazapp.banca_zapp.entity.Visita;
import com.bancazapp.banca_zapp.entity.VisitaItem;
import com.bancazapp.banca_zapp.exception.ResourceNotFoundException;
import com.bancazapp.banca_zapp.mapper.VisitaMapper;
import com.bancazapp.banca_zapp.repository.ClienteRepository;
import com.bancazapp.banca_zapp.repository.EstoqueClienteRepository;
import com.bancazapp.banca_zapp.repository.ProdutoRepository;
import com.bancazapp.banca_zapp.repository.VisitaRepository;

@Service
public class VisitaService {

    private final VisitaRepository visitaRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueClienteRepository estoqueClienteRepository;
    private final VisitaMapper visitaMapper;

    public VisitaService(VisitaRepository visitaRepository,
                         ClienteRepository clienteRepository,
                         ProdutoRepository produtoRepository,
                         EstoqueClienteRepository estoqueClienteRepository,
                         VisitaMapper visitaMapper) {
        this.visitaRepository = visitaRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueClienteRepository = estoqueClienteRepository;
        this.visitaMapper = visitaMapper;
    }

    @Transactional
    public VisitaDto registrarVisita(VisitaDto dto) {
        Cliente cliente = buscarCliente(dto.getClienteId());

        Visita visita = new Visita();
        visita.setCliente(cliente);
        visita.setDataVisita(dto.getDataVisita());
        visita.setObservacoes(dto.getObservacoes());

        List<VisitaItem> itens = new ArrayList<>();
        for (VisitaItemDto itemDto : dto.getItens()) {
            Produto produto = buscarProduto(itemDto.getProdutoId());
            VisitaItem item = new VisitaItem();
            item.setProduto(produto);
            item.setVisita(visita);
            item.setPossuia(itemDto.getPossuia());
            item.setEntregue(itemDto.getEntregue());
            item.setVendido(itemDto.getVendido());
            item.setRetirado(itemDto.getRetirado());
            item.setPossuiAgora(itemDto.getPossuiAgora());
            itens.add(item);
        }
        visita.setItens(itens);

        Visita salva = visitaRepository.save(visita);

        // Atualiza o estoque do cliente com base no saldo atual informado na visita.
        for (VisitaItemDto itemDto : dto.getItens()) {
            Produto produto = buscarProduto(itemDto.getProdutoId());
            EstoqueCliente estoque = estoqueClienteRepository
                    .findByClienteIdAndProdutoId(cliente.getId(), produto.getId())
                    .orElseGet(() -> {
                        EstoqueCliente novo = new EstoqueCliente();
                        novo.setCliente(cliente);
                        novo.setProduto(produto);
                        return novo;
                    });
            estoque.setQuantidade(itemDto.getPossuiAgora());
            estoqueClienteRepository.save(estoque);
        }

        return visitaMapper.toDto(salva);
    }

    public List<VisitaDto> listarPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente nao encontrado: " + clienteId);
        }
        return visitaRepository.findByClienteId(clienteId).stream()
                .map(visitaMapper::toDto)
                .toList();
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + id));
    }

    private Produto buscarProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado: " + id));
    }
}
