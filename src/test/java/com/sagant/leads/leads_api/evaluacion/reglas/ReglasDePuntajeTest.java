package com.sagant.leads.leads_api.evaluacion.reglas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sagant.leads.leads_api.evaluacion.dominio.Clearing;
import com.sagant.leads.leads_api.evaluacion.dominio.Destino;
import com.sagant.leads.leads_api.evaluacion.dominio.Documentacion;
import com.sagant.leads.leads_api.evaluacion.dominio.Motivo;
import com.sagant.leads.leads_api.evaluacion.dominio.SituacionLaboral;
import com.sagant.leads.leads_api.evaluacion.dominio.Solicitud;
import com.sagant.leads.leads_api.evaluacion.dominio.SolicitudDePrueba;
import com.sagant.leads.leads_api.evaluacion.dominio.Urgencia;

/** Tabla de casos por regla de negocio, con el DRL y la configuración reales. */
class ReglasDePuntajeTest {

	@ParameterizedTest(name = "urgencia {0} -> {1} ({2})")
	@CsvSource({
			"ESTA_SEMANA, esta-semana, 20",
			"ESTE_MES, este-mes, 15",
			"DE_1_A_3_MESES, 1-a-3-meses, 5",
			"SIN_APURO, sin-apuro, 0",
	})
	void urgencia(Urgencia urgencia, String codigo, int puntos) {
		assertMotivo(SolicitudDePrueba.base().urgencia(urgencia).build(), "urgencia", codigo, puntos);
	}

	@ParameterizedTest(name = "documentacion {0} -> {1} ({2})")
	@CsvSource({ "SI, si, 15", "NO, no, 0" })
	void documentacion(Documentacion documentacion, String codigo, int puntos) {
		assertMotivo(SolicitudDePrueba.base().documentacion(documentacion).build(), "documentacion", codigo, puntos);
	}

	@ParameterizedTest(name = "destino {0} -> {1} ({2})")
	@CsvSource({
			"AUTO, definido, 5", "VIVIENDA, definido, 5", "DEUDAS, definido, 5", "NEGOCIO, definido, 5",
			"ESTUDIO, definido, 5", "SALUD, definido, 5", "VIAJE, definido, 5", "OTRO, no-definido, 0",
	})
	void destino(Destino destino, String codigo, int puntos) {
		assertMotivo(SolicitudDePrueba.base().destino(destino).build(), "destino", codigo, puntos);
	}

	@ParameterizedTest(name = "situacion {0} -> {1} ({2})")
	@CsvSource({
			"DEPENDIENTE, dependiente, 15",
			"INDEPENDIENTE, independiente, 10",
			"JUBILADO, jubilado, 15",
			"ESTUDIANTE, estudiante, 0",
			"DESEMPLEADO, desempleado, -10",
	})
	void situacionLaboral(SituacionLaboral situacion, String codigo, int puntos) {
		assertMotivo(SolicitudDePrueba.base().situacionLaboral(situacion).antiguedadMeses(0).build(),
				"situacion-laboral", codigo, puntos);
	}

	@ParameterizedTest(name = "{0} con {1} meses -> {2} ({3})")
	@CsvSource({
			"DEPENDIENTE, 0, baja, 0",
			"DEPENDIENTE, 5, baja, 0",
			"DEPENDIENTE, 6, media, 5",
			"DEPENDIENTE, 11, media, 5",
			"DEPENDIENTE, 12, alta, 10",
			"DEPENDIENTE, 600, alta, 10",
			"INDEPENDIENTE, 5, baja, 0",
			"INDEPENDIENTE, 6, media, 5",
			"INDEPENDIENTE, 11, media, 5",
			"INDEPENDIENTE, 12, alta, 10",
			"JUBILADO, 0, no-aplica-jubilado, 0",
			"JUBILADO, 600, no-aplica-jubilado, 0",
			"DESEMPLEADO, 12, no-aplica-desempleado, 0",
			"ESTUDIANTE, 12, no-aplica-estudiante, 0",
	})
	void antiguedad(SituacionLaboral situacion, int meses, String codigo, int puntos) {
		assertMotivo(SolicitudDePrueba.base().situacionLaboral(situacion).antiguedadMeses(meses).build(),
				"antiguedad", codigo, puntos);
	}

	@ParameterizedTest(name = "clearing {0} -> {1} ({2})")
	@CsvSource({ "SI, si, -30", "NO_SE, no-se, -5", "NO, no, 0" })
	void clearing(Clearing clearing, String codigo, int puntos) {
		assertMotivo(SolicitudDePrueba.base().clearing(clearing).build(), "clearing", codigo, puntos);
	}

	private static void assertMotivo(Solicitud solicitud, String regla, String codigo, int puntos) {
		List<Motivo> deLaRegla = ReglasDePrueba.motor().evaluar(solicitud).stream()
				.filter(m -> m.regla().equals(regla)).toList();
		assertThat(deLaRegla).containsExactly(new Motivo(regla, codigo, puntos));
	}
}
