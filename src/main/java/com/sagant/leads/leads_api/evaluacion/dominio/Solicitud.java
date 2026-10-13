package com.sagant.leads.leads_api.evaluacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Solicitud ya validada. Es el hecho principal que reciben las reglas del DRL.
 *
 * <p>Los importes son {@link BigDecimal}; los métodos de comparación usan {@code compareTo}, sin
 * dividir ni redondear, para que los bordes de los umbrales sean exactos.
 */
public record Solicitud(
		String sessionId,
		int edad,
		Destino destino,
		BigDecimal monto,
		int plazoMeses,
		Urgencia urgencia,
		SituacionLaboral situacionLaboral,
		int antiguedadMeses,
		BigDecimal ingresoMensual,
		BigDecimal cuotaDeudas,
		Clearing clearing,
		Documentacion documentacion) {

	public Solicitud {
		Objects.requireNonNull(destino);
		Objects.requireNonNull(monto);
		Objects.requireNonNull(urgencia);
		Objects.requireNonNull(situacionLaboral);
		Objects.requireNonNull(ingresoMensual);
		Objects.requireNonNull(cuotaDeudas);
		Objects.requireNonNull(clearing);
		Objects.requireNonNull(documentacion);
		if (plazoMeses <= 0) {
			throw new IllegalArgumentException("plazoMeses debe ser mayor que 0");
		}
	}

	/**
	 * Indica si la carga de deuda no supera la fracción del ingreso:
	 * {@code (cuotaDeudas + monto / plazoMeses) / ingresoMensual <= fraccion}.
	 *
	 * <p>Se compara sin dividir, multiplicando ambos lados por {@code plazoMeses * ingresoMensual}:
	 * {@code cuotaDeudas * plazoMeses + monto <= fraccion * ingresoMensual * plazoMeses}. Con
	 * ingreso 0 el lado derecho es 0 y el izquierdo es positivo (monto > 0), así que nunca se
	 * cumple: cae en el tramo más alto sin un caso especial.
	 */
	public boolean cargaNoSupera(BigDecimal fraccion) {
		BigDecimal plazo = BigDecimal.valueOf(plazoMeses);
		BigDecimal deudaPorPlazo = cuotaDeudas.multiply(plazo).add(monto);
		BigDecimal limite = fraccion.multiply(ingresoMensual).multiply(plazo);
		return deudaPorPlazo.compareTo(limite) <= 0;
	}

	/** Indica si la antigüedad laboral es de al menos {@code meses}. */
	public boolean antiguedadAlMenos(BigDecimal meses) {
		return BigDecimal.valueOf(antiguedadMeses).compareTo(meses) >= 0;
	}
}
