package com.sagant.leads.leads_api.evaluacion.enums;

/**
 * Separa las reglas excluyentes de las de puntaje: un motivo de ELEGIBILIDAD dice si la solicitud
 * es elegible y no suma puntos; uno de PUNTAJE suma (o resta) al score.
 */
public enum TipoMotivo {
	ELEGIBILIDAD, PUNTAJE
}
