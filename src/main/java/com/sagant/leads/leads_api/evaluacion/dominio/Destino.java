package com.sagant.leads.leads_api.evaluacion.dominio;

/** Destino del préstamo (campo `motivo` del contrato). */
public enum Destino implements CodigoContrato {

	AUTO("auto"),
	VIVIENDA("vivienda"),
	DEUDAS("deudas"),
	NEGOCIO("negocio"),
	ESTUDIO("estudio"),
	SALUD("salud"),
	VIAJE("viaje"),
	OTRO("otro");

	private final String codigo;

	Destino(String codigo) {
		this.codigo = codigo;
	}

	@Override
	public String codigo() {
		return codigo;
	}
}
