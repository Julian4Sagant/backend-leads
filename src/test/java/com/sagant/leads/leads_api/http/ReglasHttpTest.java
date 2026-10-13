package com.sagant.leads.leads_api.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tareas 6.8 a 6.10: cada regla de puntaje de punta a punta por HTTP. */
class ReglasHttpTest extends PruebaHttp {

	// ---- 6.8 Urgencia, documentación y destino ----

	@Test
	@DisplayName("Urgencia esta semana")
	void urgenciaEstaSemana() throws Exception {
		assertIncluyeMotivo(evaluar(base()), "Urgencia esta semana: +20");
	}

	@Test
	@DisplayName("Urgencia este mes")
	void urgenciaEsteMes() throws Exception {
		Respuesta r = evaluar(con("urgencia", "este_mes"));
		assertEvaluacion(r, 80, "ALTA");
		assertIncluyeMotivo(r, "Urgencia este mes: +15");
	}

	@Test
	@DisplayName("Urgencia de 1 a 3 meses")
	void urgenciaDe1A3Meses() throws Exception {
		Respuesta r = evaluar(con("urgencia", "1_a_3_meses"));
		assertEvaluacion(r, 70, "ALTA");
		assertIncluyeMotivo(r, "Urgencia de 1 a 3 meses: +5");
	}

	@Test
	@DisplayName("Sin urgencia")
	void sinUrgencia() throws Exception {
		Respuesta r = evaluar(con("urgencia", "sin_apuro"));
		assertEvaluacion(r, 65, "ALTA");
		assertIncluyeMotivo(r, "Sin urgencia: 0");
	}

	@Test
	@DisplayName("Urgencia inválida no se evalúa")
	void urgenciaInvalidaNoSeEvalua() throws Exception {
		Respuesta r = evaluar(con("urgencia", "ya"));
		assertValidacion(r, "urgencia", "valor-no-permitido");
		org.assertj.core.api.Assertions.assertThat(r.json().has("motivos")).isFalse();
	}

	@Test
	@DisplayName("Con documentación")
	void conDocumentacion() throws Exception {
		assertIncluyeMotivo(evaluar(base()), "Documentación de ingresos disponible: +15");
	}

	@Test
	@DisplayName("Sin documentación")
	void sinDocumentacion() throws Exception {
		Respuesta r = evaluar(con("documentacion", "no"));
		assertEvaluacion(r, 70, "ALTA");
		assertIncluyeMotivo(r, "Sin documentación de ingresos: 0");
	}

	@Test
	@DisplayName("Documentación inválida")
	void documentacionInvalida() throws Exception {
		assertValidacion(evaluar(con("documentacion", "tal_vez")), "documentacion", "valor-no-permitido");
	}

	@Test
	@DisplayName("Destino definido")
	void destinoDefinido() throws Exception {
		assertIncluyeMotivo(evaluar(base()), "Destino del préstamo definido: +5");
	}

	@Test
	@DisplayName("Destino no definido")
	void destinoNoDefinido() throws Exception {
		Respuesta r = evaluar(con("motivo", "otro"));
		assertEvaluacion(r, 80, "ALTA");
		assertIncluyeMotivo(r, "Destino del préstamo no definido: 0");
	}

	@Test
	@DisplayName("Destino inválido")
	void destinoInvalido() throws Exception {
		assertValidacion(evaluar(con("motivo", "casa")), "motivo", "valor-no-permitido");
	}

	// ---- 6.9 Situación laboral y antigüedad ----

	@Test
	@DisplayName("Dependiente")
	void dependiente() throws Exception {
		assertIncluyeMotivo(evaluar(base()), "Empleado en relación de dependencia: +15");
	}

	@Test
	@DisplayName("Independiente")
	void independiente() throws Exception {
		Respuesta r = evaluar(con("situacion_laboral", "independiente"));
		assertEvaluacion(r, 80, "ALTA");
		assertIncluyeMotivo(r, "Trabajador independiente: +10");
	}

	@Test
	@DisplayName("Jubilado")
	void jubilado() throws Exception {
		Respuesta r = evaluar(con("situacion_laboral", "jubilado", "antiguedad_meses", 0));
		assertEvaluacion(r, 75, "ALTA");
		assertIncluyeMotivo(r, "Jubilado: +15");
	}

	@Test
	@DisplayName("Estudiante")
	void estudiante() throws Exception {
		Respuesta r = evaluar(con("situacion_laboral", "estudiante", "antiguedad_meses", 0));
		assertEvaluacion(r, 60, "ALTA");
		assertIncluyeMotivo(r, "Estudiante: 0");
	}

	@Test
	@DisplayName("Desempleado")
	void desempleado() throws Exception {
		Respuesta r = evaluar(con("situacion_laboral", "desempleado", "antiguedad_meses", 0));
		assertEvaluacion(r, 50, "MEDIA");
		assertIncluyeMotivo(r, "Desempleado: -10");
	}

	@Test
	@DisplayName("Situación laboral inválida")
	void situacionLaboralInvalida() throws Exception {
		assertValidacion(evaluar(con("situacion_laboral", "monotributista")), "situacion_laboral", "valor-no-permitido");
	}

	@Test
	@DisplayName("Antigüedad de 12 meses")
	void antiguedadDe12Meses() throws Exception {
		Respuesta r = evaluar(con("antiguedad_meses", 12));
		assertEvaluacion(r, 85, "ALTA");
		assertIncluyeMotivo(r, "Antigüedad laboral de 12 meses o más: +10");
	}

	@Test
	@DisplayName("Antigüedad de 11 meses")
	void antiguedadDe11Meses() throws Exception {
		Respuesta r = evaluar(con("antiguedad_meses", 11));
		assertEvaluacion(r, 80, "ALTA");
		assertIncluyeMotivo(r, "Antigüedad laboral de 6 a 11 meses: +5");
	}

	@Test
	@DisplayName("Antigüedad de 6 meses")
	void antiguedadDe6Meses() throws Exception {
		Respuesta r = evaluar(con("antiguedad_meses", 6));
		assertEvaluacion(r, 80, "ALTA");
		assertIncluyeMotivo(r, "Antigüedad laboral de 6 a 11 meses: +5");
	}

	@Test
	@DisplayName("Antigüedad de 5 meses")
	void antiguedadDe5Meses() throws Exception {
		Respuesta r = evaluar(con("antiguedad_meses", 5));
		assertEvaluacion(r, 75, "ALTA");
		assertIncluyeMotivo(r, "Antigüedad laboral de menos de 6 meses: 0");
	}

	@Test
	@DisplayName("Antigüedad de independiente")
	void antiguedadDeIndependiente() throws Exception {
		assertIncluyeMotivo(evaluar(con("situacion_laboral", "independiente", "antiguedad_meses", 12)),
				"Antigüedad laboral de 12 meses o más: +10");
	}

	@Test
	@DisplayName("Antigüedad no aplica a jubilado")
	void antiguedadNoAplicaAJubilado() throws Exception {
		Respuesta r = evaluar(con("situacion_laboral", "jubilado", "antiguedad_meses", 600));
		assertEvaluacion(r, 75, "ALTA");
		assertIncluyeMotivo(r, "Antigüedad laboral no aplica a jubilado: 0");
	}

	@Test
	@DisplayName("Antigüedad no aplica a desempleado")
	void antiguedadNoAplicaADesempleado() throws Exception {
		assertIncluyeMotivo(evaluar(con("situacion_laboral", "desempleado", "antiguedad_meses", 0)),
				"Antigüedad laboral no aplica a desempleado: 0");
	}

	@Test
	@DisplayName("Antigüedad no aplica a estudiante")
	void antiguedadNoAplicaAEstudiante() throws Exception {
		assertIncluyeMotivo(evaluar(con("situacion_laboral", "estudiante", "antiguedad_meses", 0)),
				"Antigüedad laboral no aplica a estudiante: 0");
	}

	@Test
	@DisplayName("Antigüedad que no aplica se valida igual")
	void antiguedadQueNoAplicaSeValidaIgual() throws Exception {
		assertValidacion(evaluar(con("situacion_laboral", "jubilado", "antiguedad_meses", 601)), "antiguedad_meses",
				"fuera-de-rango");
	}

	// ---- 6.10 Clearing y carga ----

	@Test
	@DisplayName("No figura en el Clearing")
	void noFiguraEnElClearing() throws Exception {
		assertIncluyeMotivo(evaluar(base()), "No figura en el Clearing de Informes: 0");
	}

	@Test
	@DisplayName("Figura en el Clearing")
	void figuraEnElClearing() throws Exception {
		Respuesta r = evaluar(con("clearing", "si"));
		assertEvaluacion(r, 55, "MEDIA");
		assertIncluyeMotivo(r, "Figura en el Clearing de Informes: -30");
	}

	@Test
	@DisplayName("No sabe si figura en el Clearing")
	void noSabeSiFiguraEnElClearing() throws Exception {
		Respuesta r = evaluar(con("clearing", "no_se"));
		assertEvaluacion(r, 80, "ALTA");
		assertIncluyeMotivo(r, "No sabe si figura en el Clearing de Informes: -5");
	}

	@Test
	@DisplayName("Clearing inválido")
	void clearingInvalido() throws Exception {
		assertValidacion(evaluar(con("clearing", "No")), "clearing", "valor-no-permitido");
	}

	@Test
	@DisplayName("Carga exactamente 0,3")
	void cargaExactamente03() throws Exception {
		assertIncluyeMotivo(evaluar(carga("2000")), "Carga de deuda hasta el 30% del ingreso: +20");
	}

	@Test
	@DisplayName("Carga apenas mayor que 0,3")
	void cargaApenasMayorQue03() throws Exception {
		assertIncluyeMotivo(evaluar(carga("2000.01")), "Carga de deuda de más del 30% y hasta el 50% del ingreso: +5");
	}

	@Test
	@DisplayName("Carga exactamente 0,5")
	void cargaExactamente05() throws Exception {
		assertIncluyeMotivo(evaluar(carga("4000")), "Carga de deuda de más del 30% y hasta el 50% del ingreso: +5");
	}

	@Test
	@DisplayName("Carga apenas mayor que 0,5")
	void cargaApenasMayorQue05() throws Exception {
		assertIncluyeMotivo(evaluar(carga("4000.01")), "Carga de deuda de más del 50% del ingreso: -15");
	}

	@Test
	@DisplayName("Ingreso cero cae en el tramo más alto")
	void ingresoCeroCaeEnElTramoMasAlto() throws Exception {
		assertIncluyeMotivo(evaluar(con("ingreso_mensual", 0, "cuota_deudas", 0)),
				"Carga de deuda de más del 50% del ingreso: -15");
	}

	@Test
	@DisplayName("Carga sin plazo válido")
	void cargaSinPlazoValido() throws Exception {
		Respuesta r = evaluar(con("plazo_meses", 0));
		assertValidacion(r, "plazo_meses", "fuera-de-rango");
		org.assertj.core.api.Assertions.assertThat(r.json().has("motivos")).isFalse();
	}

	/** Monto 24000 a 24 meses (1000 por mes) e ingreso 10000; la cuota como string decimal exacto. */
	private static java.util.Map<String, Object> carga(String cuota) {
		return con("monto", 24000, "plazo_meses", 24, "ingreso_mensual", 10000, "cuota_deudas", cuota);
	}
}
