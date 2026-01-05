package com.example.springhotel.exception;

public class ChambreNonDisponibleException extends RuntimeException {
    public ChambreNonDisponibleException(String message) {
        super(message);
    }
}