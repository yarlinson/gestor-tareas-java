package com.yarlinson.gestortareas.common;

/** Se superó un límite de intentos; se responde 429 con la cabecera Retry-After. */
public class TooManyRequestsException extends RuntimeException {

    private final long segundosDeEspera;

    public TooManyRequestsException(String mensaje, long segundosDeEspera) {
        super(mensaje);
        this.segundosDeEspera = segundosDeEspera;
    }

    public long getSegundosDeEspera() {
        return segundosDeEspera;
    }
}
