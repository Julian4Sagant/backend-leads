package com.sagant.leads.leads_api.evaluacion.service;

import com.sagant.leads.leads_api.evaluacion.model.Motivo;

/** Traduce los motivos emitidos por las reglas a los textos legibles de la respuesta. */
public interface RedaccionMotivos {

	/**
	 * Texto completo del motivo: con su puntaje si es de puntaje (por ejemplo "Urgencia esta
	 * semana: +20") y sin puntaje si es de elegibilidad.
	 */
	String texto(Motivo motivo);

	/** Motivo final cuando la suma se recorta a 0. */
	String recorteAlMinimo(int suma);

	/** Motivo final cuando la suma se recorta a 100. */
	String recorteAlMaximo(int suma);

	/** Motivo final cuando la solicitud no es elegible y el score se ajusta a 0. */
	String ajustePorInelegibilidad(int suma);
}
