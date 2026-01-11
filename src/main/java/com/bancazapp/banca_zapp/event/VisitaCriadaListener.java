package com.bancazapp.banca_zapp.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.bancazapp.banca_zapp.entity.Visita;
import com.bancazapp.banca_zapp.repository.VisitaRepository;
import com.bancazapp.banca_zapp.service.EmailService;

@Component
public class VisitaCriadaListener {

    private final VisitaRepository visitaRepository;
    private final EmailService emailService;

    public VisitaCriadaListener(VisitaRepository visitaRepository, EmailService emailService) {
        this.visitaRepository = visitaRepository;
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitaCriada(VisitaCriadaEvent event) {
        Visita visita = visitaRepository.findWithItensById(event.getVisitaId()).orElse(null);
        if (visita == null) {
            return;
        }
        emailService.enviarVisitaCriada(visita);
    }
}
