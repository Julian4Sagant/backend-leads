package com.sagant.leads.leads_api.evaluacion.model;

import java.util.Objects;

/**
 * Lo que emite una regla del DRL: la regla de negocio ({@code @regla}), el código del tramo que
 * disparó y sus puntos. El texto en español lo arma Java a partir de esto.
 */
public record Motivo(String regla, String codigo, int puntos) {

	public Motivo {
		Objects.requireNonNull(regla);
		Objects.requireNonNull(codigo);
	}
}
