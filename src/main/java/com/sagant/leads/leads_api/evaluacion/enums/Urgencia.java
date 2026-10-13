package com.sagant.leads.leads_api.evaluacion.enums;

/** Urgencia declarada por la persona. */
public enum Urgencia implements CodigoContrato {

	ESTA_SEMANA("esta_semana"),
	ESTE_MES("este_mes"),
	DE_1_A_3_MESES("1_a_3_meses"),
	SIN_APURO("sin_apuro");

	private final String codigo;

	Urgencia(String codigo) {
		this.codigo = codigo;
	}

	@Override
	public String codigo() {
		return codigo;
	}
}
