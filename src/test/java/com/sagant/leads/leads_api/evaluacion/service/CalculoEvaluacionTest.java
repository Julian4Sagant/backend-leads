package com.sagant.leads.leads_api.evaluacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sagant.leads.leads_api.evaluacion.enums.Prioridad;
import com.sagant.leads.leads_api.evaluacion.model.Evaluacion;
import com.sagant.leads.leads_api.evaluacion.model.Motivo;

/**
 * Suma, recorte, ajuste por inelegibilidad y prioridad. Los motivos se arman a mano para llegar a
 * sumas que la configuración real no alcanza (más de 100) o no produce (59 y 29: los puntajes
 * reales son múltiplos de 5).
 */
class CalculoEvaluacionTest {

	private final CalculoEvaluacion calculo = new CalculoEvaluacion(60, 30,
			new TextosMotivos(ReglasDePrueba.propiedades()));

	@ParameterizedTest(name = "elegible, suma {0} -> score {1}, {2}, recorte: {3}")
	@CsvSource(delimiter = '|', value = {
			"-55|0|BAJA|Score limitado al mínimo de 0 (suma -55)",
			"-1|0|BAJA|Score limitado al mínimo de 0 (suma -1)",
			"0|0|BAJA|",
			"29|29|BAJA|",
			"30|30|MEDIA|",
			"59|59|MEDIA|",
			"60|60|ALTA|",
			"100|100|ALTA|",
			"101|100|ALTA|Score limitado al máximo de 100 (suma 101)",
			"105|100|ALTA|Score limitado al máximo de 100 (suma 105)",
	})
	void scoreRecorteYPrioridad(int suma, int score, Prioridad prioridad, String recorte) {
		Evaluacion evaluacion = calculo.calcular(motivosQueSuman(suma));
		assertThat(evaluacion.score()).isEqualTo(score);
		assertThat(evaluacion.prioridad()).isEqualTo(prioridad);
		assertThat(evaluacion.motivos()).noneMatch(m -> m.startsWith("Score ajustado"));
		if (recorte == null) {
			assertThat(evaluacion.motivos()).hasSize(8);
		}
		else {
			assertThat(evaluacion.motivos()).hasSize(9).last().isEqualTo(recorte);
		}
	}

	@ParameterizedTest(name = "no elegible, suma {0} -> score 0, BAJA y ajuste")
	@CsvSource({ "-55", "0", "85", "105" })
	void noElegibleSiempreScore0YBajaConUnSoloAjuste(int suma) {
		Evaluacion evaluacion = calculo.calcular(motivosQueSuman(suma, Motivo.NO_ELEGIBLE));
		assertThat(evaluacion.score()).isZero();
		assertThat(evaluacion.prioridad()).isEqualTo(Prioridad.BAJA);
		assertThat(evaluacion.motivos()).hasSize(9)
				.last().isEqualTo("Score ajustado a 0 por inelegibilidad (suma " + suma + ")");
		assertThat(evaluacion.motivos()).filteredOn(m -> m.startsWith("Score ")).hasSize(1);
	}

	@Test
	void noElegibleConUmbralDeMediaEn0EsBaja() {
		var conMediaEn0 = new CalculoEvaluacion(60, 0, new TextosMotivos(ReglasDePrueba.propiedades()));
		Evaluacion evaluacion = conMediaEn0.calcular(motivosQueSuman(85, Motivo.NO_ELEGIBLE));
		assertThat(evaluacion.score()).isZero();
		assertThat(evaluacion.prioridad()).isEqualTo(Prioridad.BAJA);
	}

	@Test
	void faltaUnaRegla() {
		List<Motivo> motivos = new ArrayList<>(motivosQueSuman(10));
		motivos.removeIf(m -> m.regla().equals("clearing"));
		assertThatThrownBy(() -> calculo.calcular(motivos)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void faltaLaReglaDeEdad() {
		List<Motivo> motivos = new ArrayList<>(motivosQueSuman(10));
		motivos.removeIf(m -> m.regla().equals("edad"));
		assertThatThrownBy(() -> calculo.calcular(motivos)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void unaReglaRepetida() {
		List<Motivo> motivos = new ArrayList<>(motivosQueSuman(10));
		motivos.add(new Motivo("carga", "baja", 0));
		assertThatThrownBy(() -> calculo.calcular(motivos)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void laEdadRepetidaEsUnError() {
		List<Motivo> motivos = new ArrayList<>(motivosQueSuman(10));
		motivos.add(Motivo.elegibilidad("edad", Motivo.NO_ELEGIBLE));
		assertThatThrownBy(() -> calculo.calcular(motivos)).isInstanceOf(IllegalStateException.class);
	}

	/** Solicitud elegible: un motivo por regla; toda la suma va en urgencia y el resto suma 0. */
	static List<Motivo> motivosQueSuman(int suma) {
		return motivosQueSuman(suma, Motivo.ELEGIBLE);
	}

	/** Como {@link #motivosQueSuman(int)}, con el código de elegibilidad por edad indicado. */
	static List<Motivo> motivosQueSuman(int suma, String codigoEdad) {
		return List.of(
				Motivo.elegibilidad("edad", codigoEdad),
				new Motivo("urgencia", "esta-semana", suma),
				new Motivo("documentacion", "no", 0),
				new Motivo("destino", "no-definido", 0),
				new Motivo("situacion-laboral", "estudiante", 0),
				new Motivo("antiguedad", "no-aplica-estudiante", 0),
				new Motivo("clearing", "no", 0),
				new Motivo("carga", "media", 0));
	}
}
