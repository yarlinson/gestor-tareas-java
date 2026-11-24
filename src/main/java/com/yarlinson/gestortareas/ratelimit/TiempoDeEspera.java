package com.yarlinson.gestortareas.ratelimit;

final class TiempoDeEspera {

    private static final long NANOS_POR_SEGUNDO = 1_000_000_000L;

    private TiempoDeEspera() {
    }

    /** Convierte nanosegundos a segundos enteros, redondeando hacia arriba y con un mínimo de 1. */
    static long enSegundos(long nanosegundos) {
        return Math.max(1, (nanosegundos + NANOS_POR_SEGUNDO - 1) / NANOS_POR_SEGUNDO);
    }
}
