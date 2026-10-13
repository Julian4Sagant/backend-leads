package com.sagant.leads.leads_api.comun.error;

import java.util.List;

import com.sagant.leads.leads_api.evaluacion.api.ErrorCampo;

/** La solicitud tiene uno o más errores de campo; se responden todos juntos. */
public class ValidacionException extends RuntimeException {

	private final List<ErrorCampo> errores;

	public ValidacionException(List<ErrorCampo> errores) {
		super("La solicitud tiene " + errores.size() + " errores de validación", null, false, false);
		if (errores.isEmpty()) {
			throw new IllegalArgumentException("Una ValidacionException necesita al menos un error");
		}
		this.errores = List.copyOf(errores);
	}

	public List<ErrorCampo> errores() {
		return errores;
	}
}
