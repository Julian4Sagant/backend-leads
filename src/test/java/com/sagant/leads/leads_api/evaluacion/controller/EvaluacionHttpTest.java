package com.sagant.leads.leads_api.evaluacion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tarea 6.3: evaluación de una solicitud válida, forma de la respuesta y determinismo. */
class EvaluacionHttpTest extends PruebaHttp {

	@Test
	@DisplayName("Solicitud base evaluada")
	void solicitudBaseEvaluada() throws Exception {
		Respuesta r = evaluar(base());
		assertEvaluacion(r, 85, "ALTA");
		assertThat(r.motivos()).hasSize(8);
		assertThat(r.json().path("version_reglas").asString()).isNotBlank();
	}

	@Test
	@DisplayName("Campo extra ignorado")
	void campoExtraIgnorado() throws Exception {
		Respuesta conExtra = evaluar(con("nombre", "Persona Ficticia"));
		assertOk(conExtra);
		assertThat(conExtra.cuerpo()).isEqualTo(evaluar(base()).cuerpo());
	}

	@Test
	@DisplayName("Cuerpo que no es un objeto")
	void cuerpoQueNoEsUnObjeto() throws Exception {
		Respuesta r = evaluarCrudo("[1, 2]");
		assertValidacion(r, "$", "tipo-invalido");
		assertThat(r.errores()).hasSize(1);
	}

	@Test
	@DisplayName("Claves de la respuesta exitosa")
	void clavesDeLaRespuestaExitosa() throws Exception {
		Respuesta r = evaluar(base());
		assertOk(r);
		List<String> claves = new ArrayList<>(r.json().propertyNames());
		assertThat(claves).containsExactly("prioridad", "score", "motivos", "version_reglas");
		assertThat(r.json().path("score").isInt()).isTrue();
	}

	@Test
	@DisplayName("Respuesta de error sin claves de evaluación")
	void respuestaDeErrorSinClavesDeEvaluacion() throws Exception {
		Respuesta r = evaluar(con("edad", SIN));
		assertThat(r.status()).isEqualTo(400);
		assertThat(r.json().has("prioridad") || r.json().has("score") || r.json().has("motivos")
				|| r.json().has("version_reglas")).isFalse();
	}

	@Test
	@DisplayName("Reintento con la misma solicitud")
	void reintentoConLaMismaSolicitud() throws Exception {
		Respuesta primera = evaluar(base());
		Respuesta segunda = evaluar(base());
		assertOk(primera);
		assertOk(segunda);
		assertThat(segunda.cuerpo()).isEqualTo(primera.cuerpo());
	}

	@Test
	@DisplayName("Reintento de una solicitud inválida")
	void reintentoDeUnaSolicitudInvalida() throws Exception {
		Respuesta primera = evaluar(con("edad", 101));
		Respuesta segunda = evaluar(con("edad", 101));
		assertThat(primera.status()).isEqualTo(400);
		assertThat(segunda.status()).isEqualTo(400);
		assertThat(segunda.cuerpo()).isEqualTo(primera.cuerpo());
	}

	@Test
	@DisplayName("Configuración coherente")
	void configuracionCoherente() throws Exception {
		assertOk(evaluar(base()));
	}
}
