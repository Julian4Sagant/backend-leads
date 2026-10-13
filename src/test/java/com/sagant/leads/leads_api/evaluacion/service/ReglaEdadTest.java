package com.sagant.leads.leads_api.evaluacion.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;
import com.sagant.leads.leads_api.evaluacion.model.Motivo;
import com.sagant.leads.leads_api.evaluacion.model.SolicitudDePrueba;

/** Regla excluyente de edad con el DRL real, en los bordes de la edad mínima. */
class ReglaEdadTest {

	@ParameterizedTest(name = "edad mínima {0}, edad {1} -> {2}")
	@CsvSource({
			"18, 1, no-elegible",
			"18, 17, no-elegible",
			"18, 18, elegible",
			"18, 100, elegible",
			"21, 20, no-elegible",
			"21, 21, elegible",
	})
	void elegibilidadEnLosBordes(String edadMinima, int edad, String codigo) {
		List<Motivo> deEdad = motorConEdadMinima(edadMinima).evaluar(SolicitudDePrueba.base().edad(edad).build())
				.stream().filter(m -> m.regla().equals("edad")).toList();
		assertThat(deEdad).containsExactly(Motivo.elegibilidad("edad", codigo));
	}

	private static MotorReglas motorConEdadMinima(String edadMinima) {
		ReglasProperties base = ReglasDePrueba.propiedades();
		var umbrales = new ReglasProperties.Umbrales(base.umbrales().antiguedad(), base.umbrales().carga(),
				new BigDecimal(edadMinima));
		return new MotorReglas(ReglasDePrueba.kieBase(),
				new ReglasProperties(base.archivo(), base.prioridad(), umbrales, base.puntajes()));
	}
}
