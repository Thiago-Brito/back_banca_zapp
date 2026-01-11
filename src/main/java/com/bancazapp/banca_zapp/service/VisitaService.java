package com.bancazapp.banca_zapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.bancazapp.banca_zapp.dto.VisitaDto;
import com.bancazapp.banca_zapp.dto.VisitaItemDto;
import com.bancazapp.banca_zapp.entity.Cliente;
import com.bancazapp.banca_zapp.entity.EstoqueCliente;
import com.bancazapp.banca_zapp.entity.Produto;
import com.bancazapp.banca_zapp.entity.TipoVisita;
import com.bancazapp.banca_zapp.entity.Visita;
import com.bancazapp.banca_zapp.entity.VisitaItem;
import com.bancazapp.banca_zapp.event.VisitaCriadaEvent;
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
    private final ApplicationEventPublisher eventPublisher;

    public VisitaService(VisitaRepository visitaRepository,
                         ClienteRepository clienteRepository,
                         ProdutoRepository produtoRepository,
                         EstoqueClienteRepository estoqueClienteRepository,
                         VisitaMapper visitaMapper,
                         ApplicationEventPublisher eventPublisher) {
        this.visitaRepository = visitaRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueClienteRepository = estoqueClienteRepository;
        this.visitaMapper = visitaMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VisitaDto registrarVisita(VisitaDto dto) {
        Cliente cliente = buscarCliente(dto.getClienteId());
        TipoVisita tipoVisita = resolverTipo(dto);

        Visita visita = new Visita();
        visita.setCliente(cliente);
        visita.setDataVisita(dto.getDataVisita());
        visita.setObservacoes(dto.getObservacoes());
        visita.setTipo(tipoVisita);

        List<VisitaItem> itens = new ArrayList<>();
        for (VisitaItemDto itemDto : dto.getItens()) {
            Produto produto = buscarProduto(itemDto.getProdutoId());
            aplicarRegras(tipoVisita, itemDto);
            int possuiAgora = calcularPossuiAgora(itemDto);
            VisitaItem item = new VisitaItem();
            item.setProduto(produto);
            item.setVisita(visita);
            item.setPossuia(itemDto.getPossuia());
            item.setEntregue(itemDto.getEntregue());
            item.setVendido(itemDto.getVendido());
            item.setRetirado(itemDto.getRetirado());
            item.setPossuiAgora(possuiAgora);
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
            estoque.setQuantidade(calcularPossuiAgora(itemDto));
            estoqueClienteRepository.save(estoque);
        }

        eventPublisher.publishEvent(new VisitaCriadaEvent(salva.getId()));
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

    public VisitaDto buscarPorId(UUID id) {
        Visita visita = visitaRepository.findWithItensById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visita nao encontrada: " + id));
        return visitaMapper.toDto(visita);
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + id));
    }

    private Produto buscarProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado: " + id));
    }

    private TipoVisita resolverTipo(VisitaDto dto) {
        if (dto.getTipo() != null) {
            validarTipoNosItens(dto, dto.getTipo());
            return dto.getTipo();
        }
        TipoVisita encontrado = null;
        for (VisitaItemDto item : dto.getItens()) {
            if (item.getTipo() != null) {
                if (encontrado == null) {
                    encontrado = item.getTipo();
                } else if (encontrado != item.getTipo()) {
                    throw new IllegalArgumentException("Itens com tipos diferentes nao sao permitidos.");
                }
            }
        }
        if (encontrado == null) {
            throw new IllegalArgumentException("Tipo da visita e obrigatorio.");
        }
        return encontrado;
    }

    private void validarTipoNosItens(VisitaDto dto, TipoVisita tipo) {
        boolean conflito = dto.getItens().stream()
                .anyMatch(item -> item.getTipo() != null && item.getTipo() != tipo);
        if (conflito) {
            throw new IllegalArgumentException("Tipo informado na visita diverge dos itens.");
        }
    }

    private void aplicarRegras(TipoVisita tipo, VisitaItemDto itemDto) {
        if (tipo == TipoVisita.VENDA) {
            if (itemDto.getVendido() == null || itemDto.getVendido() <= 0) {
                throw new IllegalArgumentException("Visita de venda exige vendido > 0.");
            }
            itemDto.setEntregue(0);
            itemDto.setRetirado(0);
        } else {
            boolean semMovimento = (itemDto.getEntregue() == null || itemDto.getEntregue() <= 0)
                    && (itemDto.getRetirado() == null || itemDto.getRetirado() <= 0);
            if (semMovimento) {
                throw new IllegalArgumentException("Visita de entrega/devolucao exige entregue ou retirado > 0.");
            }
            itemDto.setVendido(0);
        }
    }

    private int calcularPossuiAgora(VisitaItemDto itemDto) {
        int possuia = valor(itemDto.getPossuia());
        int entregue = valor(itemDto.getEntregue());
        int vendido = valor(itemDto.getVendido());
        int retirado = valor(itemDto.getRetirado());
        return possuia + entregue - vendido - retirado;
    }

    private int valor(Integer value) {
        return value == null ? 0 : value;
    }
}
