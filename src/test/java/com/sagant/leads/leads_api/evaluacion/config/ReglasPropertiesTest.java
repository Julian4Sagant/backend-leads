package com.sagant.leads.leads_api.evaluacion.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Arranque con la configuración de reglas: la de application.yml y variantes incoherentes.
 * Cubre el requirement "Configuración de reglas validada al arrancar" (salvo el escenario
 * HTTP «Configuración coherente», que se prueba con el endpoint).
 */
class ReglasPropertiesTest {

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ReglasProperties.class)
	static class Config {
	}

	/** Carga application.yml (main y test) como la aplicación real. */
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(Config.class);

	@Test
	void laConfiguracionPorDefectoSeCarga() {
		runner.run(context -> {
			assertThat(context).hasNotFailed();
			ReglasProperties reglas = context.getBean(ReglasProperties.class);
			assertThat(reglas.archivo()).isEqualTo("classpath:reglas/evaluacion.drl");
			assertThat(reglas.prioridad().alta()).isEqualByComparingTo("60");
			assertThat(reglas.prioridad().media()).isEqualByComparingTo("30");
			assertThat(reglas.umbrales().antiguedad().mesesAlto()).isEqualByComparingTo("12");
			assertThat(reglas.umbrales().antiguedad().mesesMedio()).isEqualByComparingTo("6");
			assertThat(reglas.umbrales().carga().baja()).isEqualByComparingTo("0.3");
			assertThat(reglas.umbrales().carga().alta()).isEqualByComparingTo("0.5");
			assertThat(reglas.umbrales().edadMinima()).isEqualByComparingTo("18");
			var p = reglas.puntajes();
			assertThat(p.urgencia().estaSemana()).isEqualByComparingTo("20");
			assertThat(p.urgencia().esteMes()).isEqualByComparingTo("15");
			assertThat(p.urgencia().de1A3Meses()).isEqualByComparingTo("5");
			assertThat(p.urgencia().sinApuro()).isEqualByComparingTo("0");
			assertThat(p.documentacion().si()).isEqualByComparingTo("15");
			assertThat(p.documentacion().no()).isEqualByComparingTo("0");
			assertThat(p.destino().definido()).isEqualByComparingTo("5");
			assertThat(p.destino().noDefinido()).isEqualByComparingTo("0");
			assertThat(p.situacionLaboral().dependiente()).isEqualByComparingTo("15");
			assertThat(p.situacionLaboral().independiente()).isEqualByComparingTo("10");
			assertThat(p.situacionLaboral().jubilado()).isEqualByComparingTo("15");
			assertThat(p.situacionLaboral().estudiante()).isEqualByComparingTo("0");
			assertThat(p.situacionLaboral().desempleado()).isEqualByComparingTo("-10");
			assertThat(p.antiguedad().doceOMas()).isEqualByComparingTo("10");
			assertThat(p.antiguedad().seisAOnce()).isEqualByComparingTo("5");
			assertThat(p.antiguedad().menosDeSeis()).isEqualByComparingTo("0");
			assertThat(p.antiguedad().noAplica()).isEqualByComparingTo("0");
			assertThat(p.clearing().si()).isEqualByComparingTo("-30");
			assertThat(p.clearing().noSe()).isEqualByComparingTo("-5");
			assertThat(p.clearing().no()).isEqualByComparingTo("0");
			assertThat(p.carga().baja()).isEqualByComparingTo("20");
			assertThat(p.carga().media()).isEqualByComparingTo("5");
			assertThat(p.carga().alta()).isEqualByComparingTo("-15");
		});
	}

	@Test
	@DisplayName("Umbral de ALTA no mayor que el de MEDIA")
	void umbralDeAltaNoMayorQueElDeMedia() {
		runner.withPropertyValues("leads.reglas.prioridad.alta=30", "leads.reglas.prioridad.media=30")
				.run(context -> assertFallaNombrando(context, "leads.reglas.prioridad.alta"));
	}

	@Test
	@DisplayName("Umbrales de carga invertidos")
	void umbralesDeCargaInvertidos() {
		runner.withPropertyValues("leads.reglas.umbrales.carga.baja=0.5", "leads.reglas.umbrales.carga.alta=0.3")
				.run(context -> assertFallaNombrando(context, "leads.reglas.umbrales.carga.baja"));
	}

	@Test
	@DisplayName("Puntaje faltante")
	void puntajeFaltante() {
		runner.withPropertyValues("leads.reglas.puntajes.urgencia.esta-semana=")
				.run(context -> assertFallaNombrando(context, "leads.reglas.puntajes.urgencia.estaSemana"));
	}

	@Test
	@DisplayName("Edad mínima faltante")
	void edadMinimaFaltante() {
		runner.withPropertyValues("leads.reglas.umbrales.edad-minima=")
				.run(context -> assertFallaNombrando(context, "leads.reglas.umbrales.edadMinima"));
	}

	@Test
	@DisplayName("Edad mínima 0")
	void edadMinima0() {
		runner.withPropertyValues("leads.reglas.umbrales.edad-minima=0")
				.run(context -> assertFallaNombrando(context, "leads.reglas.umbrales.edadMinima"));
	}

	@Test
	@DisplayName("Edad mínima 101")
	void edadMinima101() {
		runner.withPropertyValues("leads.reglas.umbrales.edad-minima=101")
				.run(context -> assertFallaNombrando(context, "leads.reglas.umbrales.edadMinima"));
	}

	@Test
	@DisplayName("Edad mínima con decimales")
	void edadMinimaConDecimales() {
		runner.withPropertyValues("leads.reglas.umbrales.edad-minima=18.5")
				.run(context -> assertFallaNombrando(context, "leads.reglas.umbrales.edadMinima"));
	}

	/** Bordes válidos de la edad mínima: 1 y 100 arrancan; 18.0 se lee como 18. */
	@ParameterizedTest(name = "edad mínima {0} arranca")
	@CsvSource({ "1", "100", "18.0" })
	void edadMinimaEnLosBordesArranca(String valor) {
		runner.withPropertyValues("leads.reglas.umbrales.edad-minima=" + valor).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(ReglasProperties.class).umbrales().edadMinima()).isEqualByComparingTo(valor);
		});
	}

	/** Borde exacto de cada condición de coherencia y del requisito de puntajes enteros. */
	@ParameterizedTest(name = "{0}={1}, {2}={3} -> arranca: {4}")
	@CsvSource({
			"leads.reglas.prioridad.alta, 30, leads.reglas.prioridad.media, 30, false",
			"leads.reglas.prioridad.alta, 31, leads.reglas.prioridad.media, 30, true",
			"leads.reglas.prioridad.alta, 60, leads.reglas.prioridad.media, -1, false",
			"leads.reglas.prioridad.alta, 60, leads.reglas.prioridad.media, 0, true",
			"leads.reglas.umbrales.carga.baja, 0.5, leads.reglas.umbrales.carga.alta, 0.5, false",
			"leads.reglas.umbrales.carga.baja, 0.49, leads.reglas.umbrales.carga.alta, 0.5, true",
			"leads.reglas.umbrales.carga.baja, 0, leads.reglas.umbrales.carga.alta, 0.5, false",
			"leads.reglas.umbrales.antiguedad.meses-medio, 12, leads.reglas.umbrales.antiguedad.meses-alto, 12, false",
			"leads.reglas.umbrales.antiguedad.meses-medio, 11, leads.reglas.umbrales.antiguedad.meses-alto, 12, true",
			"leads.reglas.umbrales.antiguedad.meses-medio, 6.5, leads.reglas.umbrales.antiguedad.meses-alto, 12, false",
			"leads.reglas.puntajes.urgencia.esta-semana, 20.5, leads.reglas.puntajes.urgencia.este-mes, 15, false",
			"leads.reglas.puntajes.urgencia.esta-semana, 20.0, leads.reglas.puntajes.urgencia.este-mes, 15, true",
	})
	void bordesDeCoherencia(String clave1, String valor1, String clave2, String valor2, boolean arranca) {
		runner.withPropertyValues(clave1 + "=" + valor1, clave2 + "=" + valor2).run(context -> {
			if (arranca) {
				assertThat(context).hasNotFailed();
			}
			else {
				assertThat(context).hasFailed();
			}
		});
	}

	@Test
	void veinteYVeintePuntoCeroSeLeenIgual() {
		runner.withPropertyValues("leads.reglas.puntajes.urgencia.esta-semana=20.0").run(context -> {
			assertThat(context).hasNotFailed();
			BigDecimal valor = context.getBean(ReglasProperties.class).puntajes().urgencia().estaSemana();
			assertThat(valor).isEqualByComparingTo("20");
		});
	}

	private static void assertFallaNombrando(AssertableApplicationContext context, String propiedad) {
		assertThat(context).hasFailed();
		StringBuilder mensajes = new StringBuilder();
		for (Throwable t = context.getStartupFailure(); t != null; t = t.getCause()) {
			mensajes.append(t.getMessage()).append('\n');
		}
		assertThat(mensajes.toString()).contains(propiedad);
	}
}
