package com.sagant.leads.leads_api.evaluacion.service;

import java.util.List;

import org.kie.api.KieBase;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.stereotype.Component;

import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;
import com.sagant.leads.leads_api.evaluacion.model.Motivo;
import com.sagant.leads.leads_api.evaluacion.model.Resultado;
import com.sagant.leads.leads_api.evaluacion.model.Solicitud;

/**
 * Ejecuta las reglas del DRL sobre una solicitud, con una sesión sin estado por evaluación.
 * Devuelve los motivos en el orden en que las reglas dispararon: ordenarlos es tarea de Java.
 */
@Component
public class MotorReglas {

	private final KieBase kieBase;
	private final ReglasProperties reglas;

	public MotorReglas(KieBase kieBase, ReglasProperties reglas) {
		this.kieBase = kieBase;
		this.reglas = reglas;
	}

	public List<Motivo> evaluar(Solicitud solicitud) {
		StatelessKieSession sesion = kieBase.newStatelessKieSession();
		sesion.setGlobal("umbrales", reglas.umbrales());
		sesion.setGlobal("puntajes", reglas.puntajes());
		Resultado resultado = new Resultado();
		sesion.execute(List.of(solicitud, resultado));
		return resultado.motivos();
	}
}
