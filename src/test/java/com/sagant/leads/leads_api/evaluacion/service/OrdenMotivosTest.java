package com.sagant.leads.leads_api.evaluacion.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.sagant.leads.leads_api.evaluacion.enums.Clearing;
import com.sagant.leads.leads_api.evaluacion.enums.Destino;
import com.sagant.leads.leads_api.evaluacion.enums.Documentacion;
import com.sagant.leads.leads_api.evaluacion.enums.SituacionLaboral;
import com.sagant.leads.leads_api.evaluacion.enums.Urgencia;
import com.sagant.leads.leads_api.evaluacion.model.Evaluacion;
import com.sagant.leads.leads_api.evaluacion.model.Motivo;
import com.sagant.leads.leads_api.evaluacion.model.SolicitudDePrueba;

/**
 * Orden completo de los motivos: 8 sin motivo final y 9 con recorte o con ajuste, con el de edad
 * siempre primero y el motivo final una sola vez y al final.
 */
class OrdenMotivosTest {

	private final CalculoEvaluacion calculo = new CalculoEvaluacion(60, 30,
			new TextosMotivos(ReglasDePrueba.propiedades()));

	@Test
	void ordenCompletoSinRecorte() {
		Evaluacion evaluacion = calculo.calcular(ReglasDePrueba.motor().evaluar(SolicitudDePrueba.base().build()));
		assertThat(evaluacion.motivos()).containsExactly(
				"Edad de 18 años o más: elegible",
				"Urgencia esta semana: +20",
				"Documentación de ingresos disponible: +15",
				"Destino del préstamo definido: +5",
				"Empleado en relación de dependencia: +15",
				"Antigüedad laboral de 12 meses o más: +10",
				"No figura en el Clearing de Informes: 0",
				"Carga de deuda hasta el 30% del ingreso: +20");
		assertThat(evaluacion.score()).isEqualTo(85);
	}

	@Test
	void ordenCompletoConRecorteAlMinimo() {
		var solicitud = SolicitudDePrueba.base().situacionLaboral(SituacionLaboral.DESEMPLEADO).antiguedadMeses(0)
				.urgencia(Urgencia.SIN_APURO).documentacion(Documentacion.NO).destino(Destino.OTRO)
				.clearing(Clearing.SI).cuotaDeudas("60000").build();
		Evaluacion evaluacion = calculo.calcular(ReglasDePrueba.motor().evaluar(solicitud));
		assertThat(evaluacion.motivos()).containsExactly(
				"Edad de 18 años o más: elegible",
				"Sin urgencia: 0",
				"Sin documentación de ingresos: 0",
				"Destino del préstamo no definido: 0",
				"Desempleado: -10",
				"Antigüedad laboral no aplica a desempleado: 0",
				"Figura en el Clearing de Informes: -30",
				"Carga de deuda de más del 50% del ingreso: -15",
				"Score limitado al mínimo de 0 (suma -55)");
		assertUnRecorteAlFinal(evaluacion, "Score limitado al mínimo de 0 (suma -55)");
	}

	@Test
	void ordenCompletoConRecorteAlMaximo() {
		Evaluacion evaluacion = calculo.calcular(CalculoEvaluacionTest.motivosQueSuman(105));
		assertThat(evaluacion.motivos()).hasSize(9).first().isEqualTo("Edad de 18 años o más: elegible");
		assertUnRecorteAlFinal(evaluacion, "Score limitado al máximo de 100 (suma 105)");
	}

	@Test
	void ordenCompletoConAjustePorInelegibilidad() {
		Evaluacion evaluacion = calculo.calcular(ReglasDePrueba.motor().evaluar(SolicitudDePrueba.base().edad(16).build()));
		assertThat(evaluacion.motivos()).containsExactly(
				"No elegible: menor de 18 años",
				"Urgencia esta semana: +20",
				"Documentación de ingresos disponible: +15",
				"Destino del préstamo definido: +5",
				"Empleado en relación de dependencia: +15",
				"Antigüedad laboral de 12 meses o más: +10",
				"No figura en el Clearing de Informes: 0",
				"Carga de deuda hasta el 30% del ingreso: +20",
				"Score ajustado a 0 por inelegibilidad (suma 85)");
		assertThat(evaluacion.motivos()).filteredOn(m -> m.startsWith("Score ")).hasSize(1);
	}

	@Test
	void ordenCompletoConAjusteYSumaNegativa() {
		var solicitud = SolicitudDePrueba.base().edad(16).situacionLaboral(SituacionLaboral.DESEMPLEADO)
				.antiguedadMeses(0).urgencia(Urgencia.SIN_APURO).documentacion(Documentacion.NO)
				.destino(Destino.OTRO).clearing(Clearing.SI).cuotaDeudas("60000").build();
		Evaluacion evaluacion = calculo.calcular(ReglasDePrueba.motor().evaluar(solicitud));
		assertThat(evaluacion.motivos()).hasSize(9).first().isEqualTo("No elegible: menor de 18 años");
		assertThat(evaluacion.motivos().getLast()).isEqualTo("Score ajustado a 0 por inelegibilidad (suma -55)");
		assertThat(evaluacion.motivos()).filteredOn(m -> m.startsWith("Score ")).hasSize(1);
	}

	@Test
	void elOrdenNoDependeDelOrdenDeDisparo() {
		List<Motivo> emitidos = new ArrayList<>(ReglasDePrueba.motor().evaluar(SolicitudDePrueba.base().edad(16).build()));
		List<String> esperado = calculo.calcular(emitidos).motivos();
		Collections.reverse(emitidos);
		assertThat(calculo.calcular(emitidos).motivos()).isEqualTo(esperado);
		Collections.shuffle(emitidos, new Random(7));
		assertThat(calculo.calcular(emitidos).motivos()).isEqualTo(esperado);
	}

	private static void assertUnRecorteAlFinal(Evaluacion evaluacion, String recorte) {
		List<String> motivos = evaluacion.motivos();
		assertThat(motivos.getLast()).isEqualTo(recorte);
		assertThat(motivos.stream().filter(m -> m.startsWith("Score limitado"))).hasSize(1);
	}
}
