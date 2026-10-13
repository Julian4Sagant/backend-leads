package com.sagant.leads.leads_api.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tarea 6.12: el máximo por situación laboral y clearing se calcula recorriendo por HTTP todas
 * las combinaciones de códigos y tramos con la configuración efectiva, y se compara con la tabla
 * de la spec. Tramos de antigüedad: 0, 6 y 12 meses. Tramos de carga con monto 24000 a 24 meses
 * e ingreso 10000: cuota 2000 (baja), 3000 (media) y 5000 (alta).
 */
class TablaMaximosHttpTest extends PruebaHttp {

	private static final List<String> URGENCIAS = List.of("esta_semana", "este_mes", "1_a_3_meses", "sin_apuro");
	private static final List<String> DOCUMENTACION = List.of("si", "no");
	private static final List<String> DESTINOS = List.of("auto", "vivienda", "deudas", "negocio", "estudio", "salud",
			"viaje", "otro");
	private static final List<Integer> ANTIGUEDADES = List.of(0, 6, 12);
	private static final List<Integer> CUOTAS = List.of(2000, 3000, 5000);
	private static final List<String> SIN_CLEARING = List.of("no", "no_se");
	private static final List<String> CON_CLEARING = List.of("si");

	@Test
	@DisplayName("Máximo de dependiente")
	void maximoDeDependiente() throws Exception {
		assertMaximo("dependiente", SIN_CLEARING, 85, "ALTA");
	}

	@Test
	@DisplayName("Máximo de dependiente con clearing")
	void maximoDeDependienteConClearing() throws Exception {
		assertMaximo("dependiente", CON_CLEARING, 55, "MEDIA");
	}

	@Test
	@DisplayName("Máximo de independiente")
	void maximoDeIndependiente() throws Exception {
		assertMaximo("independiente", SIN_CLEARING, 80, "ALTA");
	}

	@Test
	@DisplayName("Máximo de independiente con clearing")
	void maximoDeIndependienteConClearing() throws Exception {
		assertMaximo("independiente", CON_CLEARING, 50, "MEDIA");
	}

	@Test
	@DisplayName("Máximo de jubilado")
	void maximoDeJubilado() throws Exception {
		assertMaximo("jubilado", SIN_CLEARING, 75, "ALTA");
	}

	@Test
	@DisplayName("Máximo de jubilado con clearing")
	void maximoDeJubiladoConClearing() throws Exception {
		assertMaximo("jubilado", CON_CLEARING, 45, "MEDIA");
	}

	@Test
	@DisplayName("Máximo de estudiante")
	void maximoDeEstudiante() throws Exception {
		assertMaximo("estudiante", SIN_CLEARING, 60, "ALTA");
	}

	@Test
	@DisplayName("Máximo de estudiante con clearing")
	void maximoDeEstudianteConClearing() throws Exception {
		assertMaximo("estudiante", CON_CLEARING, 30, "MEDIA");
	}

	@Test
	@DisplayName("Máximo de desempleado")
	void maximoDeDesempleado() throws Exception {
		assertMaximo("desempleado", SIN_CLEARING, 50, "MEDIA");
	}

	@Test
	@DisplayName("Máximo de desempleado con clearing")
	void maximoDeDesempleadoConClearing() throws Exception {
		assertMaximo("desempleado", CON_CLEARING, 20, "BAJA");
	}

	@Test
	@DisplayName("Máximo de estudiante en el borde de ALTA")
	void maximoDeEstudianteEnElBordeDeAlta() throws Exception {
		assertEvaluacion(evaluar(con("situacion_laboral", "estudiante", "antiguedad_meses", 0)), 60, "ALTA");
	}

	@Test
	@DisplayName("Máximo con clearing no llega a ALTA")
	void maximoConClearingNoLlegaAAlta() throws Exception {
		assertEvaluacion(evaluar(con("clearing", "si")), 55, "MEDIA");
	}

	@Test
	@DisplayName("Combinación inválida fuera de la tabla")
	void combinacionInvalidaFueraDeLaTabla() throws Exception {
		assertValidacion(evaluar(con("situacion_laboral", "jubilada")), "situacion_laboral", "valor-no-permitido");
	}

	private void assertMaximo(String situacion, List<String> clearings, int maximo, String prioridad) throws Exception {
		int mejor = Integer.MIN_VALUE;
		String prioridadDelMejor = null;
		for (String urgencia : URGENCIAS) {
			for (String documentacion : DOCUMENTACION) {
				for (String destino : DESTINOS) {
					for (int antiguedad : ANTIGUEDADES) {
						for (int cuota : CUOTAS) {
							for (String clearing : clearings) {
								Map<String, Object> solicitud = con("situacion_laboral", situacion, "urgencia", urgencia,
										"documentacion", documentacion, "motivo", destino, "antiguedad_meses", antiguedad,
										"clearing", clearing, "monto", 24000, "plazo_meses", 24, "ingreso_mensual", 10000,
										"cuota_deudas", cuota);
								Respuesta r = evaluar(solicitud);
								assertThat(r.status()).as("%s -> %s", solicitud, r.cuerpo()).isEqualTo(200);
								if (r.score() > mejor) {
									mejor = r.score();
									prioridadDelMejor = r.prioridad();
								}
							}
						}
					}
				}
			}
		}
		assertThat(mejor).isEqualTo(maximo);
		assertThat(prioridadDelMejor).isEqualTo(prioridad);
	}
}
