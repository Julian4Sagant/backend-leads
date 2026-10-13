package com.sagant.leads.leads_api.evaluacion.api;

import java.util.List;
import java.util.Optional;

import com.sagant.leads.leads_api.evaluacion.dominio.Solicitud;

/**
 * Resultado de validar una solicitud: la solicitud tipada si no hubo errores, o la lista de
 * errores en orden de campo. {@code sessionIdValido} está presente si el {@code session_id}
 * cumple su formato, aunque otros campos tengan errores (se usa para el log).
 */
public record ResultadoValidacion(Optional<Solicitud> solicitud, List<ErrorCampo> errores,
		Optional<String> sessionIdValido) {

	public ResultadoValidacion {
		errores = List.copyOf(errores);
		if (solicitud.isPresent() == !errores.isEmpty()) {
			throw new IllegalArgumentException("Debe haber una solicitud o errores, no ambos ni ninguno");
		}
	}

	public boolean esValida() {
		return solicitud.isPresent();
	}
}
