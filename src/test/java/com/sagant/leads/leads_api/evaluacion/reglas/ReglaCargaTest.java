package com.sagant.leads.leads_api.evaluacion.reglas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sagant.leads.leads_api.evaluacion.dominio.Motivo;
import com.sagant.leads.leads_api.evaluacion.dominio.SolicitudDePrueba;

/**
 * Bordes de la regla de carga con el DRL y la configuración reales. Base: monto 24000 a 24 meses
 * (1000 por mes) e ingreso 10000, así que la cuota 2000 da carga 0,3 exacta y 4000 da 0,5.
 */
class ReglaCargaTest {

	@ParameterizedTest(name = "ingreso {0}, cuota {1} -> {2} ({3})")
	@CsvSource({
			"10000, 2000, baja, 20",
			"10000, 2000.01, media, 5",
			"10000, 4000, media, 5",
			"10000, 4000.01, alta, -15",
			"0, 0, alta, -15",
	})
	void bordesDeCarga(String ingreso, String cuota, String codigo, int puntos) {
		var solicitud = SolicitudDePrueba.base().monto("24000").plazoMeses(24).ingresoMensual(ingreso)
				.cuotaDeudas(cuota).build();
		List<Motivo> carga = ReglasDePrueba.motor().evaluar(solicitud).stream()
				.filter(m -> m.regla().equals("carga")).toList();
		assertThat(carga).containsExactly(new Motivo("carga", codigo, puntos));
	}
}
