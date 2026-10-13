package com.sagant.leads.leads_api.evaluacion.reglas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;

/**
 * Test de cierre: si cambia la lista de reglas del DRL, hay que subir
 * {@link VersionReglas#REVISION_LOGICA} y actualizar acá la lista y la revisión guardadas.
 */
class CierreVersionReglasTest {

	/** Revisión con la que se guardó {@link #REGLAS_GUARDADAS}. */
	private static final int REVISION_GUARDADA = 1;

	/** Reglas del DRL como {@code nombre@regla}, ordenadas. */
	private static final List<String> REGLAS_GUARDADAS = List.of(
			"antiguedad-alta@antiguedad",
			"antiguedad-baja@antiguedad",
			"antiguedad-media@antiguedad",
			"antiguedad-no-aplica@antiguedad",
			"carga-alta@carga",
			"carga-baja@carga",
			"carga-media@carga",
			"clearing-no-se@clearing",
			"clearing-no@clearing",
			"clearing-si@clearing",
			"destino-definido@destino",
			"destino-no-definido@destino",
			"documentacion-no@documentacion",
			"documentacion-si@documentacion",
			"situacion-dependiente@situacion-laboral",
			"situacion-desempleado@situacion-laboral",
			"situacion-estudiante@situacion-laboral",
			"situacion-independiente@situacion-laboral",
			"situacion-jubilado@situacion-laboral",
			"urgencia-1-a-3-meses@urgencia",
			"urgencia-esta-semana@urgencia",
			"urgencia-este-mes@urgencia",
			"urgencia-sin-apuro@urgencia");

	@Test
	void laListaDeReglasYLaRevisionEstanAlDia() {
		assertThat(problemaDeCierre(reglasDe(ReglasDePrueba.kieBase()), VersionReglas.REVISION_LOGICA)).isEmpty();
	}

	@Test
	void fallaSiSeAgregaUnaReglaSinSubirLaRevision() {
		List<String> conUnaMas = new ArrayList<>(REGLAS_GUARDADAS);
		conUnaMas.add("urgencia-hoy@urgencia");
		assertThat(problemaDeCierre(conUnaMas, REVISION_GUARDADA)).hasValueSatisfying(
				m -> assertThat(m).contains("subí VersionReglas.REVISION_LOGICA"));
	}

	@Test
	void conLaRevisionSubidaSoloPideActualizarLaLista() {
		List<String> conUnaMas = new ArrayList<>(REGLAS_GUARDADAS);
		conUnaMas.add("urgencia-hoy@urgencia");
		assertThat(problemaDeCierre(conUnaMas, REVISION_GUARDADA + 1)).hasValueSatisfying(
				m -> assertThat(m).contains("actualizá REGLAS_GUARDADAS").doesNotContain("subí"));
	}

	/**
	 * Vacío si la lista y la revisión coinciden con las guardadas. Si la lista cambió con la misma
	 * revisión, el problema es que no se subió la revisión.
	 */
	static Optional<String> problemaDeCierre(List<String> reglas, int revision) {
		boolean mismaLista = reglas.stream().sorted().toList().equals(REGLAS_GUARDADAS);
		boolean mismaRevision = revision == REVISION_GUARDADA;
		if (mismaLista && mismaRevision) {
			return Optional.empty();
		}
		if (!mismaLista && mismaRevision) {
			return Optional.of("Cambió la lista de reglas del DRL: subí VersionReglas.REVISION_LOGICA y "
					+ "actualizá REGLAS_GUARDADAS y REVISION_GUARDADA. Reglas actuales: " + reglas);
		}
		return Optional.of("La revisión es " + revision + ": actualizá REGLAS_GUARDADAS y REVISION_GUARDADA "
				+ "con las reglas actuales: " + reglas);
	}

	private static List<String> reglasDe(KieBase kieBase) {
		return kieBase.getKiePackage("reglas").getRules().stream()
				.map(r -> r.getName() + "@" + r.getMetaData().get("regla"))
				.sorted()
				.toList();
	}
}
