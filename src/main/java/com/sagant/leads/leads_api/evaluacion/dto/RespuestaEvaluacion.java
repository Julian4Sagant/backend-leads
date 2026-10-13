package com.sagant.leads.leads_api.evaluacion.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Respuesta 200 de POST /evaluar. */
@JsonPropertyOrder({ "prioridad", "score", "motivos", "version_reglas" })
public record RespuestaEvaluacion(
		String prioridad,
		int score,
		List<String> motivos,
		@JsonProperty("version_reglas") String versionReglas) {
}
