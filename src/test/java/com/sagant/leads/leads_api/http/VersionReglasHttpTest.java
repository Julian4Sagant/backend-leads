package com.sagant.leads.leads_api.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.sagant.leads.leads_api.evaluacion.reglas.ReglasDePrueba;
import com.sagant.leads.leads_api.evaluacion.reglas.VersionReglas;

/**
 * Tarea 6.13. La versión por defecto se calcula con la configuración y el DRL reales; cada
 * variante (puntaje, escritura del número, DRL con una regla más, DRL con CRLF) es un contexto
 * propio.
 */
class VersionReglasHttpTest extends PruebaHttp {

	static String versionPorDefecto() {
		return VersionReglas.calcular(ReglasDePrueba.propiedades(), ReglasDePrueba.drl()).valor();
	}

	@Test
	@DisplayName("Formato de version_reglas")
	void formatoDeVersionReglas() throws Exception {
		Respuesta r = evaluar(base());
		assertOk(r);
		assertThat(r.json().path("version_reglas").asString()).matches("^[0-9a-f]{12}$").isEqualTo(versionPorDefecto());
	}

	@Test
	@DisplayName("Misma configuración, misma versión")
	void mismaConfiguracionMismaVersion() throws Exception {
		Respuesta base = evaluar(base());
		Respuesta otra = evaluar(con("clearing", "si", "urgencia", "este_mes"));
		assertOk(otra);
		assertThat(otra.json().path("version_reglas").asString()).isEqualTo(base.json().path("version_reglas").asString());
	}

	@Test
	@DisplayName("Error sin versión")
	void errorSinVersion() throws Exception {
		Respuesta r = evaluar(con("monto", 0));
		assertValidacion(r, "monto", "fuera-de-rango");
		assertThat(r.json().has("version_reglas")).isFalse();
	}

	@Nested
	@TestPropertySource(properties = "leads.reglas.puntajes.documentacion.si=16")
	class ConOtroPuntaje extends PruebaHttp {

		@Test
		@DisplayName("Cambio de puntaje cambia la versión")
		void cambioDePuntajeCambiaLaVersion() throws Exception {
			Respuesta r = evaluar(base());
			assertOk(r);
			assertThat(r.json().path("version_reglas").asString()).isNotEqualTo(versionPorDefecto());
		}
	}

	@Nested
	@TestPropertySource(properties = "leads.reglas.puntajes.urgencia.esta-semana=20.0")
	class ConVeintePuntoCero extends PruebaHttp {

		@Test
		@DisplayName("20 y 20.0 dan la misma versión")
		void veinteYVeintePuntoCeroDanLaMismaVersion() throws Exception {
			Respuesta r = evaluar(base());
			assertOk(r);
			assertThat(r.json().path("version_reglas").asString()).isEqualTo(versionPorDefecto());
		}
	}

	@Nested
	class ConUnaReglaMas extends PruebaHttp {

		/** Regla de la regla de negocio "urgencia" que nunca dispara (edad < 0). */
		private static final String REGLA_EXTRA = """

				rule "urgencia-de-prueba"
				    @regla("urgencia")
				when
				    Solicitud( edad < 0 )
				    $r : Resultado()
				then
				    emitir(drools, $r, "de-prueba", puntajes.urgencia().sinApuro());
				end
				""";

		@DynamicPropertySource
		static void archivo(DynamicPropertyRegistry registro) {
			registro.add("leads.reglas.archivo", () -> archivoTemporal(ReglasDePrueba.drl() + REGLA_EXTRA));
		}

		@Test
		@DisplayName("Cambio de reglas cambia la versión")
		void cambioDeReglasCambiaLaVersion() throws Exception {
			Respuesta r = evaluar(base());
			assertEvaluacion(r, 85, "ALTA");
			assertThat(r.json().path("version_reglas").asString()).isNotEqualTo(versionPorDefecto());
		}
	}

	@Nested
	class ConFinalesDeLineaCrlf extends PruebaHttp {

		@DynamicPropertySource
		static void archivo(DynamicPropertyRegistry registro) {
			String lf = ReglasDePrueba.drl().replace("\r\n", "\n");
			registro.add("leads.reglas.archivo", () -> archivoTemporal(lf.replace("\n", "\r\n")));
		}

		@Test
		@DisplayName("Finales de línea no cambian la versión")
		void finalesDeLineaNoCambianLaVersion() throws Exception {
			Respuesta r = evaluar(base());
			assertOk(r);
			assertThat(r.json().path("version_reglas").asString()).isEqualTo(versionPorDefecto());
		}
	}

	/** Escribe el DRL en un archivo temporal y devuelve su ubicación como recurso de Spring. */
	static String archivoTemporal(String contenido) {
		try {
			Path archivo = Files.createTempFile("evaluacion-", ".drl");
			archivo.toFile().deleteOnExit();
			Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
			return archivo.toUri().toString();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
