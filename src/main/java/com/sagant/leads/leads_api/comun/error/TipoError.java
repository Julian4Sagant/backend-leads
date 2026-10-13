package com.sagant.leads.leads_api.comun.error;

import java.net.URI;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de {@code type} de error (spec {@code acceso-api}). Cada caso tiene un URN estable
 * {@code urn:leads-api:error:<caso>}; nunca se usa {@code about:blank}.
 */
public enum TipoError {

	VALIDACION("validacion", HttpStatus.BAD_REQUEST, "La solicitud tiene errores de validación"),
	JSON_INVALIDO("json-invalido", HttpStatus.BAD_REQUEST, "El cuerpo no es JSON válido"),
	NO_AUTORIZADO("no-autorizado", HttpStatus.UNAUTHORIZED, "Clave de API ausente o incorrecta"),
	NO_ENCONTRADO("no-encontrado", HttpStatus.NOT_FOUND, "Ruta no encontrada"),
	METODO_NO_PERMITIDO("metodo-no-permitido", HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido"),
	CUERPO_DEMASIADO_GRANDE("cuerpo-demasiado-grande", HttpStatus.CONTENT_TOO_LARGE, "El cuerpo supera el tamaño máximo"),
	TIPO_DE_CONTENIDO_NO_SOPORTADO("tipo-de-contenido-no-soportado", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
			"El cuerpo debe ser application/json"),
	INTERNO("interno", HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");

	private static final String PREFIJO = "urn:leads-api:error:";

	private final String caso;
	private final HttpStatus estado;
	private final String titulo;

	TipoError(String caso, HttpStatus estado, String titulo) {
		this.caso = caso;
		this.estado = estado;
		this.titulo = titulo;
	}

	public URI urn() {
		return URI.create(PREFIJO + caso);
	}

	public HttpStatus estado() {
		return estado;
	}

	public String titulo() {
		return titulo;
	}
}
