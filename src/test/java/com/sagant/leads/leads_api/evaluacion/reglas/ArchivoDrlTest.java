package com.sagant.leads.leads_api.evaluacion.reglas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * El DRL es solo ASCII (Drools corrompe otros caracteres según la codificación de la JVM) y no
 * contiene números de negocio: puntos y umbrales salen de application.yml. Se ignoran los
 * comentarios y los strings (nombres de regla, @regla y códigos), y los dígitos que forman parte
 * de un identificador como {@code de1A3Meses}.
 */
class ArchivoDrlTest {

	private static final Pattern COMENTARIO = Pattern.compile("//[^\n]*");
	private static final Pattern STRING = Pattern.compile("\"[^\"\n]*\"");
	private static final Pattern NUMERO = Pattern.compile("(?<![A-Za-z0-9_])\\d+(\\.\\d+)?(?![A-Za-z_])");

	@Test
	void elDrlRealNoTieneProblemas() {
		assertThat(problemas(ReglasDePrueba.drl())).isEmpty();
	}

	@Test
	void detectaUnCaracterNoAscii() {
		String copia = ReglasDePrueba.drl().replace("// Reglas de evaluacion", "// Reglas de evaluación");
		assertThat(problemas(copia)).anyMatch(p -> p.startsWith("no ASCII"));
	}

	@Test
	void detectaUnNumeroDeNegocio() {
		String copia = ReglasDePrueba.drl().replace("puntajes.urgencia().estaSemana()", "new java.math.BigDecimal(20)");
		assertThat(problemas(copia)).anyMatch(p -> p.startsWith("número"));
	}

	static List<String> problemas(String drl) {
		List<String> problemas = new ArrayList<>();
		for (int i = 0; i < drl.length(); i++) {
			if (drl.charAt(i) > 0x7F) {
				problemas.add("no ASCII en la posición " + i + ": " + drl.charAt(i));
			}
		}
		String codigo = STRING.matcher(COMENTARIO.matcher(drl).replaceAll("")).replaceAll("\"\"");
		Matcher numero = NUMERO.matcher(codigo);
		while (numero.find()) {
			problemas.add("número " + numero.group());
		}
		return problemas;
	}
}
