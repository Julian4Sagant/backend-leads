package com.sagant.leads.leads_api.evaluacion.reglas;

import java.util.Objects;

/** El archivo DRL cargado al arrancar: su ruta configurada y su contenido. */
public record ArchivoReglas(String ruta, String contenido) {

	public ArchivoReglas {
		Objects.requireNonNull(ruta);
		Objects.requireNonNull(contenido);
	}
}
