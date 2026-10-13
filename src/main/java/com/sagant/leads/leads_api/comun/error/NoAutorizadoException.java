package com.sagant.leads.leads_api.comun.error;

/** Petición sin clave de API o con una clave incorrecta. No lleva datos de la petición. */
public class NoAutorizadoException extends RuntimeException {

	public NoAutorizadoException() {
		super("Clave de API ausente o incorrecta", null, false, false);
	}
}
