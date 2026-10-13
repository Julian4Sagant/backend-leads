package com.sagant.leads.leads_api.evaluacion.model;

import java.util.List;

import com.sagant.leads.leads_api.evaluacion.enums.Prioridad;

/** Resultado de evaluar una solicitud: prioridad, score de 0 a 100 y motivos en orden. */
public record Evaluacion(Prioridad prioridad, int score, List<String> motivos) {

	public Evaluacion {
		motivos = List.copyOf(motivos);
	}
}
