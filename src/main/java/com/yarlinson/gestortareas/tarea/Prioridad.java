package com.yarlinson.gestortareas.tarea;

/** Importancia de una tarea. El valor numérico es el que se guarda en la base de datos. */
public enum Prioridad {
    BAJA(1),
    MEDIA(2),
    ALTA(3);

    private final int valor;

    Prioridad(int valor) {
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }

    public static Prioridad desdeValor(int valor) {
        for (Prioridad prioridad : values()) {
            if (prioridad.valor == valor) {
                return prioridad;
            }
        }
        throw new IllegalArgumentException("Valor de prioridad desconocido: " + valor);
    }
}
