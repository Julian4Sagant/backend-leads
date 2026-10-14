package com.sagant.leads.leads_api.comun.exception;

/** El cuerpo de la petición supera el tamaño máximo. No lleva datos de la petición. */
public class CuerpoDemasiadoGrandeException extends RuntimeException {

	public CuerpoDemasiadoGrandeException() {
		super("El cuerpo supera el tamaño máximo", null, false, false);
	}
}
