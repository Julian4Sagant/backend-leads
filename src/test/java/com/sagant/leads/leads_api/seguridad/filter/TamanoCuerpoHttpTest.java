package com.sagant.leads.leads_api.seguridad.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sagant.leads.leads_api.evaluacion.controller.PruebaHttp;

/**
 * Límite del cuerpo con Content-Length (MockMvc lo arma con el largo del contenido): bordes,
 * forma del 413 y su orden frente a 401, 400, 404, 405 y 415.
 */
class TamanoCuerpoHttpTest extends PruebaHttp {

	/** 20 KB que no son JSON válido. */
	private static final byte[] GRANDE_INVALIDO = ("{" + "x".repeat(20479)).getBytes(StandardCharsets.UTF_8);

	/** 20 KB de JSON válido: la solicitud base con relleno. */
	private static final byte[] GRANDE = conRelleno(20480).getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("Cuerpo de 16384 bytes")
	void cuerpoDe16384Bytes() throws Exception {
		Respuesta r = evaluarCrudo(conRelleno(16384));
		Respuesta base = evaluar(base());
		assertOk(r);
		assertThat(r.prioridad()).isEqualTo(base.prioridad());
		assertThat(r.score()).isEqualTo(base.score());
		assertThat(r.motivos()).isEqualTo(base.motivos());
	}

	@Test
	@DisplayName("Cuerpo de 16385 bytes")
	void cuerpoDe16385Bytes() throws Exception {
		Respuesta r = evaluarCrudo(conRelleno(16385));
		assertError(r, 413, "cuerpo-demasiado-grande");
		assertContentType(r, "application/problem+json;charset=UTF-8");
		assertThat(r.json().has("errores")).isFalse();
	}

	@Test
	@DisplayName("Cuerpo grande sin clave")
	void cuerpoGrandeSinClave() throws Exception {
		Respuesta r = enviar(post("/evaluar").contentType(MediaType.APPLICATION_JSON).content(GRANDE));
		assertError(r, 401, "no-autorizado");
	}

	@Test
	@DisplayName("Cuerpo grande con JSON inválido")
	void cuerpoGrandeConJsonInvalido() throws Exception {
		Respuesta r = enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.APPLICATION_JSON)
				.content(GRANDE_INVALIDO));
		assertError(r, 413, "cuerpo-demasiado-grande");
	}

	@Test
	@DisplayName("Cuerpo grande a una ruta desconocida")
	void cuerpoGrandeAUnaRutaDesconocida() throws Exception {
		Respuesta r = enviar(post("/no-existe").header("X-API-Key", clave).contentType(MediaType.APPLICATION_JSON)
				.content(GRANDE));
		assertError(r, 413, "cuerpo-demasiado-grande");
	}

	@Test
	@DisplayName("Cuerpo grande con método no soportado")
	void cuerpoGrandeConMetodoNoSoportado() throws Exception {
		Respuesta r = enviar(put("/evaluar").header("X-API-Key", clave).contentType(MediaType.APPLICATION_JSON)
				.content(GRANDE));
		assertError(r, 413, "cuerpo-demasiado-grande");
	}

	@Test
	@DisplayName("Cuerpo grande con Content-Type de texto plano")
	void cuerpoGrandeConContentTypeDeTextoPlano() throws Exception {
		Respuesta r = enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.TEXT_PLAIN)
				.content(GRANDE));
		assertError(r, 413, "cuerpo-demasiado-grande");
	}
}
