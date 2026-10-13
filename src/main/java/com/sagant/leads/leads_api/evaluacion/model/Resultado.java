package com.sagant.leads.leads_api.evaluacion.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Hecho mutable donde las reglas del DRL acumulan los motivos que emiten. */
public class Resultado {

	private final List<Motivo> motivos = new ArrayList<>();

	/** Motivo de una regla de puntaje. */
	public void agregar(String regla, String codigo, BigDecimal puntos) {
		motivos.add(new Motivo(regla, codigo, puntos.intValueExact()));
	}

	/** Motivo de una regla excluyente: {@link Motivo#ELEGIBLE} o {@link Motivo#NO_ELEGIBLE}. */
	public void agregarElegibilidad(String regla, String codigo) {
		motivos.add(Motivo.elegibilidad(regla, codigo));
	}

	public List<Motivo> motivos() {
		return List.copyOf(motivos);
	}
}
