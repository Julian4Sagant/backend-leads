package com.sagant.leads.leads_api.evaluacion.dominio;

/** Si la persona figura en el Clearing de Informes por deudas impagas. */
public enum Clearing implements CodigoContrato {

	SI("si"),
	NO("no"),
	NO_SE("no_se");

	private final String codigo;

	Clearing(String codigo) {
		this.codigo = codigo;
	}

	@Override
	public String codigo() {
		return codigo;
	}
}
