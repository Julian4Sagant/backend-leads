package com.sagant.leads.leads_api.evaluacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SolicitudTest {

	private static final BigDecimal TREINTA = new BigDecimal("0.3");
	private static final BigDecimal CINCUENTA = new BigDecimal("0.5");

	/** Mapeo explícito de cada código del contrato (decisión 10). */
	@ParameterizedTest(name = "{0} {1} -> {2}")
	@CsvSource({
			"Destino, auto, AUTO", "Destino, vivienda, VIVIENDA", "Destino, deudas, DEUDAS",
			"Destino, negocio, NEGOCIO", "Destino, estudio, ESTUDIO", "Destino, salud, SALUD",
			"Destino, viaje, VIAJE", "Destino, otro, OTRO",
			"Urgencia, esta_semana, ESTA_SEMANA", "Urgencia, este_mes, ESTE_MES",
			"Urgencia, 1_a_3_meses, DE_1_A_3_MESES", "Urgencia, sin_apuro, SIN_APURO",
			"SituacionLaboral, dependiente, DEPENDIENTE", "SituacionLaboral, independiente, INDEPENDIENTE",
			"SituacionLaboral, jubilado, JUBILADO", "SituacionLaboral, desempleado, DESEMPLEADO",
			"SituacionLaboral, estudiante, ESTUDIANTE",
			"Clearing, si, SI", "Clearing, no, NO", "Clearing, no_se, NO_SE",
			"Documentacion, si, SI", "Documentacion, no, NO",
	})
	void mapeoDeCodigos(String tipo, String codigo, String constante) {
		assertThat(desde(tipo, codigo)).map(Enum::name).contains(constante);
	}

	/** El mapeo es exacto: mayúsculas, espacios y valores desconocidos no se aceptan. */
	@ParameterizedTest(name = "{0} \"{1}\" no se acepta")
	@CsvSource(value = {
			"Destino|Auto", "Destino|' auto'", "Destino|'auto '", "Destino|casa",
			"Urgencia|1-a-3-meses", "Urgencia|ESTA_SEMANA", "Clearing|No", "Clearing|no se",
			"SituacionLaboral|jubilada", "Documentacion|tal_vez", "Documentacion|",
	}, delimiter = '|', emptyValue = "")
	void codigoNoAceptado(String tipo, String codigo) {
		assertThat(desde(tipo, codigo)).isEmpty();
	}

	/** Borde exacto de la carga: base con monto 24000 a 24 meses (1000 por mes) e ingreso 10000. */
	@ParameterizedTest(name = "cuota {0}: hasta 0,3 {1}; hasta 0,5 {2}")
	@CsvSource({
			"2000, true, true",
			"2000.01, false, true",
			"4000, false, true",
			"4000.01, false, false",
	})
	void cargaNoSuperaEnLosBordes(String cuota, boolean hasta30, boolean hasta50) {
		Solicitud s = SolicitudDePrueba.base().monto("24000").plazoMeses(24).ingresoMensual("10000")
				.cuotaDeudas(cuota).build();
		assertThat(s.cargaNoSupera(TREINTA)).isEqualTo(hasta30);
		assertThat(s.cargaNoSupera(CINCUENTA)).isEqualTo(hasta50);
	}

	@Test
	void ingresoCeroNuncaQuedaDebajoDeUnUmbral() {
		Solicitud s = SolicitudDePrueba.base().ingresoMensual("0").cuotaDeudas("0").build();
		assertThat(s.cargaNoSupera(TREINTA)).isFalse();
		assertThat(s.cargaNoSupera(CINCUENTA)).isFalse();
	}

	@ParameterizedTest(name = "antigüedad {0} meses: al menos 6 {1}; al menos 12 {2}")
	@CsvSource({ "5, false, false", "6, true, false", "11, true, false", "12, true, true" })
	void antiguedadAlMenosEnLosBordes(int meses, boolean seis, boolean doce) {
		Solicitud s = SolicitudDePrueba.base().antiguedadMeses(meses).build();
		assertThat(s.antiguedadAlMenos(new BigDecimal("6"))).isEqualTo(seis);
		assertThat(s.antiguedadAlMenos(new BigDecimal("12"))).isEqualTo(doce);
	}

	private static Optional<? extends Enum<?>> desde(String tipo, String codigo) {
		return switch (tipo) {
			case "Destino" -> CodigoContrato.desdeCodigo(Destino.class, codigo);
			case "Urgencia" -> CodigoContrato.desdeCodigo(Urgencia.class, codigo);
			case "SituacionLaboral" -> CodigoContrato.desdeCodigo(SituacionLaboral.class, codigo);
			case "Clearing" -> CodigoContrato.desdeCodigo(Clearing.class, codigo);
			case "Documentacion" -> CodigoContrato.desdeCodigo(Documentacion.class, codigo);
			default -> throw new IllegalArgumentException(tipo);
		};
	}
}
