package com.bancazapp.banca_zapp.event;

import java.util.UUID;

public class VisitaCriadaEvent {

    private final UUID visitaId;

    public VisitaCriadaEvent(UUID visitaId) {
        this.visitaId = visitaId;
    }

    public UUID getVisitaId() {
        return visitaId;
    }
}
