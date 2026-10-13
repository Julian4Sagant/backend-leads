package com.sagant.leads.leads_api.evaluacion.reglas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;
import com.sagant.leads.leads_api.evaluacion.dominio.Motivo;

/** Cada (regla, código, puntos) da el texto exacto de la spec con la configuración por defecto. */
class TextosMotivosTest {

	private final TextosMotivos textos = new TextosMotivos(ReglasDePrueba.propiedades());

	@ParameterizedTest(name = "{0}/{1} ({2}) -> {3}")
	@CsvSource(delimiter = '|', value = {
			"urgencia|esta-semana|20|Urgencia esta semana: +20",
			"urgencia|este-mes|15|Urgencia este mes: +15",
			"urgencia|1-a-3-meses|5|Urgencia de 1 a 3 meses: +5",
			"urgencia|sin-apuro|0|Sin urgencia: 0",
			"documentacion|si|15|Documentación de ingresos disponible: +15",
			"documentacion|no|0|Sin documentación de ingresos: 0",
			"destino|definido|5|Destino del préstamo definido: +5",
			"destino|no-definido|0|Destino del préstamo no definido: 0",
			"situacion-laboral|dependiente|15|Empleado en relación de dependencia: +15",
			"situacion-laboral|independiente|10|Trabajador independiente: +10",
			"situacion-laboral|jubilado|15|Jubilado: +15",
			"situacion-laboral|estudiante|0|Estudiante: 0",
			"situacion-laboral|desempleado|-10|Desempleado: -10",
			"antiguedad|alta|10|Antigüedad laboral de 12 meses o más: +10",
			"antiguedad|media|5|Antigüedad laboral de 6 a 11 meses: +5",
			"antiguedad|baja|0|Antigüedad laboral de menos de 6 meses: 0",
			"antiguedad|no-aplica-jubilado|0|Antigüedad laboral no aplica a jubilado: 0",
			"antiguedad|no-aplica-desempleado|0|Antigüedad laboral no aplica a desempleado: 0",
			"antiguedad|no-aplica-estudiante|0|Antigüedad laboral no aplica a estudiante: 0",
			"clearing|si|-30|Figura en el Clearing de Informes: -30",
			"clearing|no-se|-5|No sabe si figura en el Clearing de Informes: -5",
			"clearing|no|0|No figura en el Clearing de Informes: 0",
			"carga|baja|20|Carga de deuda hasta el 30% del ingreso: +20",
			"carga|media|5|Carga de deuda de más del 30% y hasta el 50% del ingreso: +5",
			"carga|alta|-15|Carga de deuda de más del 50% del ingreso: -15",
	})
	void textoExactoDeLaSpec(String regla, String codigo, int puntos, String esperado) {
		String texto = textos.texto(new Motivo(regla, codigo, puntos));
		assertThat(texto).isEqualTo(esperado);
		assertThat(texto).doesNotContain("−");
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({ "20, +20", "1, +1", "0, 0", "-1, -1", "-55, -55" })
	void formatoDelPuntaje(int puntos, String esperado) {
		assertThat(TextosMotivos.puntaje(puntos)).isEqualTo(esperado).doesNotContain("−");
	}

	@Test
	void textosDeRecorte() {
		assertThat(textos.recorteAlMinimo(-55)).isEqualTo("Score limitado al mínimo de 0 (suma -55)");
		assertThat(textos.recorteAlMaximo(105)).isEqualTo("Score limitado al máximo de 100 (suma 105)");
	}

	@Test
	void losTextosSiguenALosUmbralesConfigurados() {
		ReglasProperties base = ReglasDePrueba.propiedades();
		var umbrales = new ReglasProperties.Umbrales(
				new ReglasProperties.Umbrales.Antiguedad(new BigDecimal("24"), new BigDecimal("3")),
				new ReglasProperties.Umbrales.Carga(new BigDecimal("0.35"), new BigDecimal("0.6")));
		var otros = new TextosMotivos(new ReglasProperties(base.archivo(), base.prioridad(), umbrales, base.puntajes()));
		assertThat(otros.texto(new Motivo("antiguedad", "alta", 10))).isEqualTo("Antigüedad laboral de 24 meses o más: +10");
		assertThat(otros.texto(new Motivo("antiguedad", "media", 5))).isEqualTo("Antigüedad laboral de 3 a 23 meses: +5");
		assertThat(otros.texto(new Motivo("antiguedad", "baja", 0))).isEqualTo("Antigüedad laboral de menos de 3 meses: 0");
		assertThat(otros.texto(new Motivo("carga", "baja", 20))).isEqualTo("Carga de deuda hasta el 35% del ingreso: +20");
		assertThat(otros.texto(new Motivo("carga", "media", 5)))
				.isEqualTo("Carga de deuda de más del 35% y hasta el 60% del ingreso: +5");
		assertThat(otros.texto(new Motivo("carga", "alta", -15))).isEqualTo("Carga de deuda de más del 60% del ingreso: -15");
	}

	@Test
	void unMotivoSinTextoEsUnError() {
		assertThatThrownBy(() -> textos.texto(new Motivo("urgencia", "ya", 0)))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("urgencia/ya");
	}
}
