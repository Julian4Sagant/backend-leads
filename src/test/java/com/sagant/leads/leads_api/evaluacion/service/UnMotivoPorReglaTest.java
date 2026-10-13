package com.sagant.leads.leads_api.evaluacion.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.StatelessKieSession;

import com.sagant.leads.leads_api.evaluacion.enums.Clearing;
import com.sagant.leads.leads_api.evaluacion.enums.Destino;
import com.sagant.leads.leads_api.evaluacion.enums.Documentacion;
import com.sagant.leads.leads_api.evaluacion.enums.SituacionLaboral;
import com.sagant.leads.leads_api.evaluacion.enums.Urgencia;
import com.sagant.leads.leads_api.evaluacion.model.Motivo;
import com.sagant.leads.leads_api.evaluacion.model.Resultado;
import com.sagant.leads.leads_api.evaluacion.model.Solicitud;
import com.sagant.leads.leads_api.evaluacion.model.SolicitudDePrueba;

/**
 * Los tramos de cada regla de negocio no se superponen ni dejan huecos: en todas las
 * combinaciones de códigos y tramos (incluidos los bordes) cada solicitud produce exactamente
 * un motivo por regla, y todas las reglas DRL disparan alguna vez.
 */
class UnMotivoPorReglaTest {

	private static final Set<String> REGLAS = Set.of("edad", "urgencia", "documentacion", "destino",
			"situacion-laboral", "antiguedad", "clearing", "carga");

	/** Edad: bordes 17 y 18 de la edad mínima y extremos 1 y 100. */
	private static final int[] EDADES = { 1, 17, 18, 100 };

	/** Antigüedad: bordes 5, 6, 11, 12 y extremos 0 y 600. */
	private static final int[] ANTIGUEDADES = { 0, 5, 6, 11, 12, 600 };

	/** Carga con monto 24000 a 24 meses: {ingreso, cuota} en cada borde e ingreso 0. */
	private static final String[][] CARGAS = { { "10000", "2000" }, { "10000", "2000.01" },
			{ "10000", "4000" }, { "10000", "4000.01" }, { "0", "0" } };

	@Test
	void exactamenteUnMotivoPorReglaDeNegocioEnTodasLasCombinaciones() {
		var motor = ReglasDePrueba.motor();
		List<String> errores = new ArrayList<>();
		int[] evaluadas = { 0 };
		combinaciones(solicitud -> {
			evaluadas[0]++;
			List<Motivo> motivos = motor.evaluar(solicitud);
			List<String> reglas = motivos.stream().map(Motivo::regla).toList();
			if (motivos.size() != REGLAS.size() || !new HashSet<>(reglas).equals(REGLAS)) {
				errores.add(solicitud + " -> " + reglas);
			}
		});
		assertThat(evaluadas[0]).isEqualTo(EDADES.length * 4 * 2 * 8 * 5 * ANTIGUEDADES.length * 3 * CARGAS.length);
		assertThat(errores).isEmpty();
	}

	@Test
	void todasLasReglasDrlDisparanAlgunaVezYTienenUnaReglaDeNegocioConocida() {
		var kieBase = ReglasDePrueba.kieBase();
		var propiedades = ReglasDePrueba.propiedades();
		Set<String> disparadas = new HashSet<>();
		combinaciones(solicitud -> {
			StatelessKieSession sesion = kieBase.newStatelessKieSession();
			sesion.setGlobal("umbrales", propiedades.umbrales());
			sesion.setGlobal("puntajes", propiedades.puntajes());
			sesion.addEventListener(new DefaultAgendaEventListener() {
				@Override
				public void afterMatchFired(AfterMatchFiredEvent evento) {
					disparadas.add(evento.getMatch().getRule().getName());
				}
			});
			sesion.execute(List.of(solicitud, new Resultado()));
		});
		List<Rule> reglasDrl = kieBase.getKiePackage("reglas").getRules().stream().toList();
		assertThat(reglasDrl).hasSize(25);
		assertThat(reglasDrl).allSatisfy(r -> assertThat(REGLAS).contains((String) r.getMetaData().get("regla")));
		assertThat(disparadas).containsExactlyInAnyOrderElementsOf(reglasDrl.stream().map(Rule::getName).toList());
	}

	private static void combinaciones(Consumer<Solicitud> accion) {
		for (int edad : EDADES) {
			for (Urgencia urgencia : Urgencia.values()) {
				for (Documentacion documentacion : Documentacion.values()) {
					for (Destino destino : Destino.values()) {
						for (SituacionLaboral situacion : SituacionLaboral.values()) {
							for (int antiguedad : ANTIGUEDADES) {
								for (Clearing clearing : Clearing.values()) {
									for (String[] carga : CARGAS) {
										accion.accept(SolicitudDePrueba.base().edad(edad).urgencia(urgencia)
												.documentacion(documentacion).destino(destino)
												.situacionLaboral(situacion).antiguedadMeses(antiguedad)
												.clearing(clearing).monto("24000").plazoMeses(24)
												.ingresoMensual(carga[0]).cuotaDeudas(carga[1]).build());
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
