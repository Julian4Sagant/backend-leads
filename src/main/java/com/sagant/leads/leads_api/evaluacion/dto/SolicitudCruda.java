package com.sagant.leads.leads_api.evaluacion.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * El cuerpo de POST /evaluar antes de validar: por cada campo del contrato presente, su valor
 * crudo. Un campo ausente no está en el mapa. Los campos que no son del contrato se descartan al
 * leer, sin registrarse.
 */
@JsonDeserialize(using = LectorSolicitudCruda.class)
public record SolicitudCruda(boolean esObjeto, Map<String, ValorCrudo> campos) {

	/** Campos del contrato, en el orden en que se informan los errores. */
	public static final List<String> CAMPOS = List.of("session_id", "edad", "motivo", "monto", "plazo_meses",
			"urgencia", "situacion_laboral", "antiguedad_meses", "ingreso_mensual", "cuota_deudas", "clearing",
			"documentacion");

	public SolicitudCruda {
		campos = Map.copyOf(campos);
	}

	/** JSON válido cuya raíz no es un objeto: un array, un escalar o {@code null}. */
	public static SolicitudCruda noEsObjeto() {
		return new SolicitudCruda(false, Map.of());
	}

	public Optional<ValorCrudo> campo(String nombre) {
		return Optional.ofNullable(campos.get(nombre));
	}
}
