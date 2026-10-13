package com.sagant.leads.leads_api.evaluacion.dominio;

/** Si la persona tiene documentación de ingresos. */
public enum Documentacion implements CodigoContrato {

	SI("si"),
	NO("no");

	private final String codigo;

	Documentacion(String codigo) {
		this.codigo = codigo;
	}

	@Override
	public String codigo() {
		return codigo;
	}
}
