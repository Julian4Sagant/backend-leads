package com.sagant.leads.leads_api.evaluacion.service;

import org.springframework.stereotype.Component;

import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;
import com.sagant.leads.leads_api.evaluacion.model.Evaluacion;
import com.sagant.leads.leads_api.evaluacion.model.Solicitud;

/** Evalúa una solicitud válida: reglas del DRL, cálculo y versión de las reglas. */
@Component
public class Evaluador {

	private final MotorReglas motor;
	private final CalculoEvaluacion calculo;
	private final VersionReglas version;

	public Evaluador(MotorReglas motor, TextosMotivos textos, ReglasProperties reglas, VersionReglas version) {
		this.motor = motor;
		this.calculo = new CalculoEvaluacion(reglas.prioridad().alta().intValueExact(),
				reglas.prioridad().media().intValueExact(), textos);
		this.version = version;
	}

	public Evaluacion evaluar(Solicitud solicitud) {
		return calculo.calcular(motor.evaluar(solicitud));
	}

	public String version() {
		return version.valor();
	}
}
