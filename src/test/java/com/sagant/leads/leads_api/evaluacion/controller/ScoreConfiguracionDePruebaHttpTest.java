package com.sagant.leads.leads_api.evaluacion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Escenarios que la configuración real no alcanza. La solicitud base suma 85 con urgencia
 * esta_semana en +20; cambiando solo ese puntaje la suma pasa a 105, 100, 59 o 29. También el
 * umbral de MEDIA en 0. Cada caso es un contexto propio.
 */
class ScoreConfiguracionDePruebaHttpTest {

	@Nested
	@TestPropertySource(properties = "leads.reglas.puntajes.urgencia.esta-semana=40")
	class SumaDe105 extends PruebaHttp {

		@Test
		@DisplayName("Suma mayor que 100")
		void sumaMayorQue100() throws Exception {
			Respuesta r = evaluar(base());
			assertEvaluacion(r, 100, "ALTA");
			assertThat(r.motivos()).hasSize(9).last().isEqualTo("Score limitado al máximo de 100 (suma 105)");
		}

		@Test
		@DisplayName("Menor con suma mayor que 100")
		void menorConSumaMayorQue100() throws Exception {
			Respuesta r = evaluar(con("edad", 16));
			assertEvaluacion(r, 0, "BAJA");
			assertThat(r.motivos()).last().isEqualTo("Score ajustado a 0 por inelegibilidad (suma 105)");
			assertThat(r.motivos()).noneMatch(m -> m.startsWith("Score limitado"));
		}
	}

	@Nested
	@TestPropertySource(properties = "leads.reglas.puntajes.urgencia.esta-semana=35")
	class SumaDe100 extends PruebaHttp {

		@Test
		@DisplayName("Suma exactamente 100")
		void sumaExactamente100() throws Exception {
			Respuesta r = evaluar(base());
			assertEvaluacion(r, 100, "ALTA");
			assertThat(r.motivos()).hasSize(8).noneMatch(m -> m.startsWith("Score limitado"));
		}
	}

	@Nested
	@TestPropertySource(properties = "leads.reglas.puntajes.urgencia.esta-semana=-6")
	class SumaDe59 extends PruebaHttp {

		@Test
		@DisplayName("Score 59")
		void score59() throws Exception {
			assertEvaluacion(evaluar(base()), 59, "MEDIA");
		}
	}

	@Nested
	@TestPropertySource(properties = "leads.reglas.puntajes.urgencia.esta-semana=-36")
	class SumaDe29 extends PruebaHttp {

		@Test
		@DisplayName("Score 29")
		void score29() throws Exception {
			assertEvaluacion(evaluar(base()), 29, "BAJA");
		}
	}

	@Nested
	@TestPropertySource(properties = "leads.reglas.prioridad.media=0")
	class MediaEn0 extends PruebaHttp {

		@Test
		@DisplayName("Menor con prioridad media configurada en 0")
		void menorConPrioridadMediaConfiguradaEn0() throws Exception {
			assertEvaluacion(evaluar(con("edad", 16)), 0, "BAJA");
		}
	}
}
