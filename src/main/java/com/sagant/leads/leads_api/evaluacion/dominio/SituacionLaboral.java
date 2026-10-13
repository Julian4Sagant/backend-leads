package com.sagant.leads.leads_api.evaluacion.dominio;

/** Situación laboral declarada por la persona. */
public enum SituacionLaboral implements CodigoContrato {

	DEPENDIENTE("dependiente"),
	INDEPENDIENTE("independiente"),
	JUBILADO("jubilado"),
	DESEMPLEADO("desempleado"),
	ESTUDIANTE("estudiante");

	private final String codigo;

	SituacionLaboral(String codigo) {
		this.codigo = codigo;
	}

	@Override
	public String codigo() {
		return codigo;
	}
}
