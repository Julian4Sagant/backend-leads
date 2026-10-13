package com.sagant.leads.leads_api.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clave de API ({@code leads.seguridad.api-key}, que toma {@code LEADS_API_KEY}). Se valida en
 * {@link ClaveApi} y no con Bean Validation: el reporte de arranque fallido de Spring Boot
 * imprimiría el valor rechazado.
 */
@ConfigurationProperties("leads.seguridad")
public record SeguridadProperties(String apiKey) {

	/** Nunca muestra la clave. */
	@Override
	public String toString() {
		return "SeguridadProperties[apiKey=****]";
	}
}
