package com.sagant.leads.leads_api.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/** Tareas 6.14 (clave y rutas) y 6.15 (errores y contenido), salvo el error interno. */
class AccesoHttpTest extends PruebaHttp {

	// ---- 6.14 Clave y rutas ----

	@Test
	@DisplayName("Clave válida")
	void claveValida() throws Exception {
		assertOk(evaluar(base()));
	}

	@Test
	@DisplayName("Sin clave")
	void sinClave() throws Exception {
		assertError(enviar(post("/evaluar").contentType(MediaType.APPLICATION_JSON).content(json(base()))), 401,
				"no-autorizado");
	}

	@Test
	@DisplayName("Clave incorrecta")
	void claveIncorrecta() throws Exception {
		assertError(enviar(post("/evaluar").header("X-API-Key", "clave-incorrecta-de-prueba-0000000000")
				.contentType(MediaType.APPLICATION_JSON).content(json(base()))), 401, "no-autorizado");
	}

	@Test
	@DisplayName("Clave con un carácter de menos")
	void claveConUnCaracterDeMenos() throws Exception {
		assertError(enviar(post("/evaluar").header("X-API-Key", clave.substring(0, clave.length() - 1))
				.contentType(MediaType.APPLICATION_JSON).content(json(base()))), 401, "no-autorizado");
	}

	@Test
	@DisplayName("Sin clave y cuerpo inválido")
	void sinClaveYCuerpoInvalido() throws Exception {
		assertError(enviar(post("/evaluar").contentType(MediaType.APPLICATION_JSON).content("{\"edad\":")), 401,
				"no-autorizado");
	}

	@Test
	@DisplayName("Ruta desconocida con clave")
	void rutaDesconocidaConClave() throws Exception {
		assertError(enviar(get("/no-existe").header("X-API-Key", clave)), 404, "no-encontrado");
	}

	@Test
	@DisplayName("Ruta desconocida sin clave")
	void rutaDesconocidaSinClave() throws Exception {
		assertError(enviar(get("/no-existe")), 401, "no-autorizado");
	}

	@Test
	@DisplayName("Método no soportado con clave")
	void metodoNoSoportadoConClave() throws Exception {
		assertError(enviar(get("/evaluar").header("X-API-Key", clave)), 405, "metodo-no-permitido");
	}

	@Test
	@DisplayName("Método no soportado sin clave")
	void metodoNoSoportadoSinClave() throws Exception {
		assertError(enviar(get("/evaluar")), 401, "no-autorizado");
	}

	@Test
	@DisplayName("Health no expuesto")
	void healthNoExpuesto() throws Exception {
		assertError(enviar(get("/actuator/health").header("X-API-Key", clave)), 404, "no-encontrado");
	}

	@Test
	@DisplayName("Documentación OpenAPI no expuesta")
	void documentacionOpenApiNoExpuesta() throws Exception {
		assertError(enviar(get("/v3/api-docs").header("X-API-Key", clave)), 404, "no-encontrado");
	}

	@Test
	@DisplayName("Swagger UI no expuesto")
	void swaggerUiNoExpuesto() throws Exception {
		assertError(enviar(get("/swagger-ui.html").header("X-API-Key", clave)), 404, "no-encontrado");
	}

	@Nested
	@TestPropertySource(properties = "leads.seguridad.api-key=clave-ficticia-de-32-caracteres!")
	class ConClaveDe32 extends PruebaHttp {

		@Test
		@DisplayName("Clave de 32 caracteres")
		void claveDe32Caracteres() throws Exception {
			assertThat(clave).hasSize(32);
			assertOk(evaluar(base()));
		}
	}

	// ---- 6.15 Errores y contenido ----

	@Test
	@DisplayName("Error con ProblemDetail")
	void errorConProblemDetail() throws Exception {
		Respuesta r = enviar(post("/evaluar").contentType(MediaType.APPLICATION_JSON).content(json(base())));
		assertError(r, 401, "no-autorizado");
		assertContentType(r, "application/problem+json;charset=UTF-8");
		assertThat(r.json().path("title").asString()).isNotBlank();
	}

	@Test
	@DisplayName("Ningún error con about:blank")
	void ningunErrorConAboutBlank() throws Exception {
		Respuesta r = enviar(get("/no-existe").header("X-API-Key", clave));
		assertError(r, 404, "no-encontrado");
		assertThat(r.cuerpo()).doesNotContain("about:blank");
	}

	@Test
	@DisplayName("Lista de errores con un elemento")
	void listaDeErroresConUnElemento() throws Exception {
		Respuesta r = evaluar(con("edad", 101));
		assertThat(r.status()).isEqualTo(400);
		assertThat(r.type()).isEqualTo(URN + "validacion");
		assertThat(r.errores()).containsExactly("edad:fuera-de-rango");
		assertThat(r.json().path("errores").get(0).path("mensaje").asString()).isNotBlank();
	}

	@Test
	@DisplayName("Códigos con guion")
	void codigosConGuion() throws Exception {
		Respuesta r = evaluarCrudo(json(con("monto", true, "clearing", "talvez", "edad", 101, "urgencia", SIN))
				.replace("\"plazo_meses\":24", "\"plazo_meses\":12.5"));
		assertThat(r.status()).isEqualTo(400);
		assertThat(r.errores()).extracting(e -> e.substring(e.indexOf(':') + 1)).containsExactlyInAnyOrder(
				"tipo-invalido", "formato-invalido", "valor-no-permitido", "fuera-de-rango", "requerido");
	}

	@Test
	@DisplayName("JSON truncado")
	void jsonTruncado() throws Exception {
		assertJsonInvalido(evaluarCrudo("{\"edad\": 32,"));
	}

	@Test
	@DisplayName("Texto suelto")
	void textoSuelto() throws Exception {
		assertJsonInvalido(evaluarCrudo("hola"));
	}

	@Test
	@DisplayName("Cuerpo vacío")
	void cuerpoVacio() throws Exception {
		assertJsonInvalido(enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.APPLICATION_JSON)));
	}

	@Test
	@DisplayName("Número más largo que el límite del parser")
	void numeroMasLargoQueElLimiteDelParser() throws Exception {
		Respuesta r = evaluarConNumero("monto", "9".repeat(1001));
		assertThat(r.status()).isNotEqualTo(500);
		assertJsonInvalido(r);
	}

	@Test
	@DisplayName("Objeto vacío es validación")
	void objetoVacioEsValidacion() throws Exception {
		Respuesta r = evaluarCrudo("{}");
		assertThat(r.status()).isEqualTo(400);
		assertThat(r.type()).isEqualTo(URN + "validacion");
	}

	@Test
	@DisplayName("Content-Type JSON con charset")
	void contentTypeJsonConCharset() throws Exception {
		assertOk(enviar(post("/evaluar").header("X-API-Key", clave).contentType("application/json;charset=UTF-8")
				.content(json(base()))));
	}

	@Test
	@DisplayName("Sin Content-Type")
	void sinContentType() throws Exception {
		assertError(enviar(post("/evaluar").header("X-API-Key", clave).content(json(base()))), 415,
				"tipo-de-contenido-no-soportado");
	}

	@Test
	@DisplayName("Content-Type de texto plano")
	void contentTypeDeTextoPlano() throws Exception {
		assertError(enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.TEXT_PLAIN)
				.content(json(base()))), 415, "tipo-de-contenido-no-soportado");
	}

	@Test
	@DisplayName("Content-Type incorrecto sin clave")
	void contentTypeIncorrectoSinClave() throws Exception {
		assertError(enviar(post("/evaluar").contentType(MediaType.TEXT_PLAIN).content(json(base()))), 401,
				"no-autorizado");
	}

	@Test
	@DisplayName("Accept de HTML en una evaluación")
	void acceptDeHtmlEnUnaEvaluacion() throws Exception {
		Respuesta r = enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.TEXT_HTML).content(json(base())));
		assertOk(r);
		assertContentType(r, "application/json;charset=UTF-8");
		assertThat(r.cuerpo()).isEqualTo(evaluar(base()).cuerpo());
	}

	@Test
	@DisplayName("Accept de HTML en un error")
	void acceptDeHtmlEnUnError() throws Exception {
		Respuesta r = enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.TEXT_HTML).content(json(con("edad", 101))));
		assertError(r, 400, "validacion");
		assertContentType(r, "application/problem+json;charset=UTF-8");
	}

	@Test
	@DisplayName("Charset en respuesta exitosa")
	void charsetEnRespuestaExitosa() throws Exception {
		Respuesta r = evaluar(base());
		assertOk(r);
		assertContentType(r, "application/json;charset=UTF-8");
	}

	@Test
	@DisplayName("Charset en respuesta de error")
	void charsetEnRespuestaDeError() throws Exception {
		Respuesta r = evaluar(con("monto", "abc"));
		assertValidacion(r, "monto", "formato-invalido");
		assertContentType(r, "application/problem+json;charset=UTF-8");
		String mensaje = r.json().path("errores").get(0).path("mensaje").asString();
		assertThat(new String(r.bytes(), StandardCharsets.UTF_8)).contains(mensaje);
		assertThat(mensaje).isEqualTo("no es un número válido");
	}

	private static void assertJsonInvalido(Respuesta r) {
		assertError(r, 400, "json-invalido");
		assertThat(r.json().has("errores")).isFalse();
	}

	private static String json(Object cuerpo) {
		return JSON.writeValueAsString(cuerpo);
	}
}
