package com.sagant.leads.leads_api.evaluacion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Elegibilidad por edad y ajuste por inelegibilidad con la configuración real (edad mínima 18).
 * Los casos con suma 105 y MEDIA en 0 están en {@link ScoreConfiguracionDePruebaHttpTest}.
 */
class ElegibilidadHttpTest extends PruebaHttp {

	private static final String ELEGIBLE = "Edad de 18 años o más: elegible";
	private static final String NO_ELEGIBLE = "No elegible: menor de 18 años";

	@Test
	@DisplayName("Edad 17 no elegible")
	void edad17NoElegible() throws Exception {
		Respuesta r = evaluar(con("edad", 17));
		assertEvaluacion(r, 0, "BAJA");
		assertThat(r.motivos()).first().isEqualTo(NO_ELEGIBLE);
	}

	@Test
	@DisplayName("Edad 18 elegible")
	void edad18Elegible() throws Exception {
		Respuesta r = evaluar(con("edad", 18));
		assertEvaluacion(r, 85, "ALTA");
		assertThat(r.motivos()).first().isEqualTo(ELEGIBLE);
	}

	@Test
	@DisplayName("Edad 1 no elegible")
	void edad1NoElegible() throws Exception {
		Respuesta r = evaluar(con("edad", 1));
		assertEvaluacion(r, 0, "BAJA");
		assertThat(r.motivos()).first().isEqualTo(NO_ELEGIBLE);
	}

	@Test
	@DisplayName("Edad 100 elegible")
	void edad100Elegible() throws Exception {
		Respuesta r = evaluar(con("edad", 100));
		assertOk(r);
		assertThat(r.score()).isEqualTo(85);
		assertThat(r.motivos()).first().isEqualTo(ELEGIBLE);
	}

	@Test
	@DisplayName("Menor con perfil de prioridad alta")
	void menorConPerfilDePrioridadAlta() throws Exception {
		Respuesta menor = evaluar(con("edad", 16));
		assertEvaluacion(menor, 0, "BAJA");
		List<String> deLaBase = evaluar(base()).motivos().subList(1, 8);
		assertThat(menor.motivos().subList(1, 8)).isEqualTo(deLaBase);
	}

	@Test
	@DisplayName("Edad cero no se evalúa")
	void edadCeroNoSeEvalua() throws Exception {
		Respuesta r = evaluar(con("edad", 0));
		assertValidacion(r, "edad", "fuera-de-rango");
		assertThat(r.json().has("motivos")).isFalse();
	}

	@Test
	@DisplayName("Menor con suma entre 0 y 100")
	void menorConSumaEntre0Y100() throws Exception {
		Respuesta r = evaluar(con("edad", 16));
		assertEvaluacion(r, 0, "BAJA");
		assertThat(r.motivos()).last().isEqualTo("Score ajustado a 0 por inelegibilidad (suma 85)");
	}

	@Test
	@DisplayName("Menor con suma 0")
	void menorConSuma0() throws Exception {
		Respuesta r = evaluar(con("situacion_laboral", "estudiante", "antiguedad_meses", 0, "urgencia", "sin_apuro",
				"documentacion", "no", "motivo", "otro", "clearing", "no_se", "cuota_deudas", 20000, "edad", 16));
		assertOk(r);
		assertThat(r.score()).isZero();
		assertThat(r.motivos()).last().isEqualTo("Score ajustado a 0 por inelegibilidad (suma 0)");
	}

	@Test
	@DisplayName("Menor con suma negativa")
	void menorConSumaNegativa() throws Exception {
		var solicitud = ScoreHttpTest.sumaNegativaSolicitud();
		solicitud.put("edad", 16);
		Respuesta r = evaluar(solicitud);
		assertOk(r);
		assertThat(r.score()).isZero();
		assertThat(r.motivos()).last().isEqualTo("Score ajustado a 0 por inelegibilidad (suma -55)");
		assertThat(r.motivos()).noneMatch(m -> m.startsWith("Score limitado"));
	}

	@Test
	@DisplayName("Mayor de edad con suma negativa")
	void mayorDeEdadConSumaNegativa() throws Exception {
		Respuesta r = evaluar(ScoreHttpTest.sumaNegativaSolicitud());
		assertOk(r);
		assertThat(r.motivos()).last().isEqualTo("Score limitado al mínimo de 0 (suma -55)");
		assertThat(r.motivos()).noneMatch(m -> m.startsWith("Score ajustado"));
	}

	@Test
	@DisplayName("Solicitud inválida sin ajuste")
	void solicitudInvalidaSinAjuste() throws Exception {
		Respuesta r = evaluar(con("edad", 16, "clearing", SIN));
		assertValidacion(r, "clearing", "requerido");
		assertThat(r.json().has("motivos")).isFalse();
	}

	@Nested
	@TestPropertySource(properties = "leads.reglas.umbrales.edad-minima=21")
	class EdadMinima21 extends PruebaHttp {

		@Test
		@DisplayName("Edad mínima 21 con edad 20")
		void edadMinima21ConEdad20() throws Exception {
			Respuesta r = evaluar(con("edad", 20));
			assertOk(r);
			assertThat(r.score()).isZero();
			assertThat(r.motivos()).first().isEqualTo("No elegible: menor de 21 años");
		}

		@Test
		@DisplayName("Edad mínima 21 con edad 21")
		void edadMinima21ConEdad21() throws Exception {
			Respuesta r = evaluar(con("edad", 21));
			assertOk(r);
			assertThat(r.score()).isEqualTo(85);
			assertThat(r.motivos()).first().isEqualTo("Edad de 21 años o más: elegible");
		}
	}
}
