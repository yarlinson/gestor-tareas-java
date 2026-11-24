package com.yarlinson.gestortareas.tarea;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda {@link Prioridad} como número (1, 2, 3) y la reconstruye al leer. */
@Converter
public class PrioridadConverter implements AttributeConverter<Prioridad, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Prioridad prioridad) {
        return prioridad == null ? null : prioridad.getValor();
    }

    @Override
    public Prioridad convertToEntityAttribute(Integer valor) {
        return valor == null ? null : Prioridad.desdeValor(valor);
    }
}
