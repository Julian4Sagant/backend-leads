package com.sagant.leads.leads_api.evaluacion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tarea 6.11 con la configuración real. Los escenarios que necesitan una configuración de prueba
 * (sumas 100 y 105, scores 59 y 29) están en {@link ScoreConfiguracionDePruebaHttpTest}.
 */
class ScoreHttpTest extends PruebaHttp {

	private static final String U2212 = "−";

	@Test
	@DisplayName("Suma negativa")
	void sumaNegativa() throws Exception {
		Respuesta r = evaluar(sumaNegativaSolicitud());
		assertEvaluacion(r, 0, "BAJA");
		assertThat(r.motivos()).last().isEqualTo("Score limitado al mínimo de 0 (suma -55)");
		assertThat(r.motivos()).noneMatch(m -> m.startsWith("No elegible") || m.startsWith("Score ajustado"));
	}

	@Test
	@DisplayName("Suma exactamente 0")
	void sumaExactamente0() throws Exception {
		Respuesta r = evaluar(con("situacion_laboral", "estudiante", "antiguedad_meses", 0, "urgencia", "sin_apuro",
				"documentacion", "no", "motivo", "otro", "clearing", "no_se", "cuota_deudas", 20000));
		assertEvaluacion(r, 0, "BAJA");
		assertThat(r.motivos()).hasSize(8).noneMatch(m -> m.startsWith("Score limitado"));
	}

	@Test
	@DisplayName("Solicitud inválida sin score")
	void solicitudInvalidaSinScore() throws Exception {
		Respuesta r = evaluar(con("monto", SIN));
		assertValidacion(r, "monto", "requerido");
		assertThat(r.json().has("score")).isFalse();
	}

	@Test
	@DisplayName("Score 60")
	void score60() throws Exception {
		assertEvaluacion(evaluar(con("antiguedad_meses", 3, "cuota_deudas", 15000)), 60, "ALTA");
	}

	@Test
	@DisplayName("Score 30")
	void score30() throws Exception {
		assertEvaluacion(evaluar(con("situacion_laboral", "estudiante", "antiguedad_meses", 0, "urgencia", "1_a_3_meses",
				"documentacion", "no")), 30, "MEDIA");
	}

	@Test
	@DisplayName("Solicitud inválida sin prioridad")
	void solicitudInvalidaSinPrioridad() throws Exception {
		Respuesta r = evaluar(con("clearing", "quizas"));
		assertValidacion(r, "clearing", "valor-no-permitido");
		assertThat(r.json().has("prioridad")).isFalse();
	}

	@Test
	@DisplayName("Orden completo de la solicitud base")
	void ordenCompletoDeLaSolicitudBase() throws Exception {
		Respuesta r = evaluar(base());
		assertOk(r);
		assertThat(r.motivos()).containsExactly(
				"Edad de 18 años o más: elegible",
				"Urgencia esta semana: +20",
				"Documentación de ingresos disponible: +15",
				"Destino del préstamo definido: +5",
				"Empleado en relación de dependencia: +15",
				"Antigüedad laboral de 12 meses o más: +10",
				"No figura en el Clearing de Informes: 0",
				"Carga de deuda hasta el 30% del ingreso: +20");
	}

	@Test
	@DisplayName("Motivo de recorte al final y una sola vez")
	void motivoDeRecorteAlFinalYUnaSolaVez() throws Exception {
		List<String> motivos = evaluar(sumaNegativaSolicitud()).motivos();
		assertThat(motivos).hasSize(9);
		assertThat(motivos.getFirst()).isEqualTo("Edad de 18 años o más: elegible");
		assertThat(motivos.getLast()).startsWith("Score limitado");
		assertThat(motivos).filteredOn(m -> m.startsWith("Score limitado")).hasSize(1);
	}

	@Test
	@DisplayName("Motivo de ajuste al final y una sola vez")
	void motivoDeAjusteAlFinalYUnaSolaVez() throws Exception {
		List<String> motivos = evaluar(con("edad", 16)).motivos();
		assertThat(motivos).hasSize(9);
		assertThat(motivos.getFirst()).isEqualTo("No elegible: menor de 18 años");
		assertThat(motivos.getLast()).startsWith("Score ajustado");
		assertThat(motivos).filteredOn(m -> m.startsWith("Score ajustado")).hasSize(1);
		assertThat(motivos).noneMatch(m -> m.startsWith("Score limitado"));
	}

	@Test
	@DisplayName("Error sin motivos")
	void errorSinMotivos() throws Exception {
		Respuesta r = evaluar(con("urgencia", SIN));
		assertValidacion(r, "urgencia", "requerido");
		assertThat(r.json().has("motivos")).isFalse();
	}

	@Test
	@DisplayName("Puntaje negativo con guion ASCII")
	void puntajeNegativoConGuionAscii() throws Exception {
		Respuesta r = evaluar(con("clearing", "si"));
		assertIncluyeMotivo(r, "Figura en el Clearing de Informes: -30");
		assertThat(r.motivos()).noneMatch(m -> m.contains(U2212));
		assertThat(r.cuerpo()).doesNotContain(U2212);
	}

	@Test
	@DisplayName("Caracteres no ASCII en UTF-8")
	void caracteresNoAsciiEnUtf8() throws Exception {
		Respuesta r = evaluar(con("cuota_deudas", 40000, "edad", 16));
		assertOk(r);
		String decodificado = new String(r.bytes(), java.nio.charset.StandardCharsets.UTF_8);
		assertThat(decodificado).contains("Carga de deuda de más del 50% del ingreso: -15").contains("Antigüedad")
				.contains("No elegible: menor de 18 años");
	}

	@Test
	@DisplayName("Motivo de elegibilidad sin puntaje")
	void motivoDeElegibilidadSinPuntaje() throws Exception {
		Respuesta r = evaluar(base());
		assertOk(r);
		assertThat(r.motivos().getFirst()).isEqualTo("Edad de 18 años o más: elegible").doesNotMatch(".*: [+-]?\\d+$");
	}

	@Test
	@DisplayName("Error sin textos de motivos")
	void errorSinTextosDeMotivos() throws Exception {
		Respuesta r = evaluar(con("documentacion", null));
		assertValidacion(r, "documentacion", "requerido");
		assertThat(r.json().has("motivos")).isFalse();
	}

	static java.util.Map<String, Object> sumaNegativaSolicitud() {
		return con("situacion_laboral", "desempleado", "antiguedad_meses", 0, "urgencia", "sin_apuro", "documentacion",
				"no", "motivo", "otro", "clearing", "si", "cuota_deudas", 60000);
	}
}
