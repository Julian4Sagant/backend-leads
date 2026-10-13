package com.sagant.leads.leads_api.evaluacion.dominio;

import java.util.List;

/** Resultado de evaluar una solicitud: prioridad, score de 0 a 100 y motivos en orden. */
public record Evaluacion(Prioridad prioridad, int score, List<String> motivos) {

	public Evaluacion {
		motivos = List.copyOf(motivos);
	}
}
