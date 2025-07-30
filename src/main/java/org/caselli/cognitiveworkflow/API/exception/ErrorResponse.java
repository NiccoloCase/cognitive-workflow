package org.caselli.cognitiveworkflow.API.exception;

import lombok.Data;

import java.time.Instant;

/**
 * Error response model for API exceptions.
 * @author niccolocaselli
 */
@Data
public class ErrorResponse {
    private String code;
    private String message;
    private Instant timestamp;

    ErrorResponse(String code, String message, Instant timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }
}