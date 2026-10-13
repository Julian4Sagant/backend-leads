package com.sagant.leads.leads_api.evaluacion.model;

import java.util.Objects;

import com.sagant.leads.leads_api.evaluacion.enums.TipoMotivo;

/**
 * Lo que emite una regla del DRL: su tipo, la regla de negocio ({@code @regla}), el código del
 * tramo que disparó y sus puntos. El texto en español lo arma Java a partir de esto.
 *
 * <p>Un motivo de ELEGIBILIDAD tiene código {@link #ELEGIBLE} o {@link #NO_ELEGIBLE} y nunca puntos.
 */
public record Motivo(TipoMotivo tipo, String regla, String codigo, int puntos) {

	public static final String ELEGIBLE = "elegible";
	public static final String NO_ELEGIBLE = "no-elegible";

	public Motivo {
		Objects.requireNonNull(tipo);
		Objects.requireNonNull(regla);
		Objects.requireNonNull(codigo);
		if (tipo == TipoMotivo.ELEGIBILIDAD) {
			if (puntos != 0) {
				throw new IllegalArgumentException("Un motivo de elegibilidad no tiene puntos");
			}
			if (!codigo.equals(ELEGIBLE) && !codigo.equals(NO_ELEGIBLE)) {
				throw new IllegalArgumentException("Código de elegibilidad desconocido: " + codigo);
			}
		}
	}

	/** Motivo de puntaje. */
	public Motivo(String regla, String codigo, int puntos) {
		this(TipoMotivo.PUNTAJE, regla, codigo, puntos);
	}

	public static Motivo elegibilidad(String regla, String codigo) {
		return new Motivo(TipoMotivo.ELEGIBILIDAD, regla, codigo, 0);
	}

	public boolean esNoElegible() {
		return tipo == TipoMotivo.ELEGIBILIDAD && codigo.equals(NO_ELEGIBLE);
	}
}
