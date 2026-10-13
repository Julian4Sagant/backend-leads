package com.sagant.leads.leads_api.evaluacion.validation;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import com.sagant.leads.leads_api.evaluacion.dto.ResultadoValidacion;

/**
 * Guarda en el request el {@code session_id} validado, para que la línea de log del error o de la
 * evaluación pueda incluirlo. Solo se guarda si cumple su formato: un valor arbitrario del cliente
 * nunca llega al log.
 */
public final class SessionIdDelRequest {

	static final String ATRIBUTO = SessionIdDelRequest.class.getName();

	private SessionIdDelRequest() {
	}

	public static void guardar(HttpServletRequest request, ResultadoValidacion resultado) {
		resultado.sessionIdValido().ifPresent(id -> request.setAttribute(ATRIBUTO, id));
	}

	public static Optional<String> leer(HttpServletRequest request) {
		return Optional.ofNullable((String) request.getAttribute(ATRIBUTO));
	}
}
