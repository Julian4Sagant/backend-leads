package com.sagant.leads.leads_api.evaluacion.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Hecho mutable donde las reglas del DRL acumulan los motivos que emiten. */
public class Resultado {

	private final List<Motivo> motivos = new ArrayList<>();

	public void agregar(String regla, String codigo, BigDecimal puntos) {
		motivos.add(new Motivo(regla, codigo, puntos.intValueExact()));
	}

	public List<Motivo> motivos() {
		return List.copyOf(motivos);
	}
}
