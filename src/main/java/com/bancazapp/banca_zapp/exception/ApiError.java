package com.bancazapp.banca_zapp.exception;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiError {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
