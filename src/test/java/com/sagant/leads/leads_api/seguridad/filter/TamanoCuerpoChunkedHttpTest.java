package com.sagant.leads.leads_api.seguridad.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.sagant.leads.leads_api.evaluacion.controller.PruebaHttp;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Límite del cuerpo sin Content-Length, con Tomcat real: MockMvc siempre declara el largo. Un
 * cuerpo de largo desconocido hace que el cliente lo mande con Transfer-Encoding chunked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TamanoCuerpoChunkedHttpTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	/** 20 KB que no son JSON válido. */
	private static final byte[] GRANDE_INVALIDO = ("{" + "x".repeat(20479)).getBytes(StandardCharsets.UTF_8);

	/** 20 KB de JSON válido: la solicitud base con relleno. */
	private static final byte[] GRANDE = PruebaHttp.conRelleno(20480).getBytes(StandardCharsets.UTF_8);

	private final HttpClient cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

	@LocalServerPort
	private int puerto;

	@Value("${leads.seguridad.api-key}")
	private String clave;

	@Test
	@DisplayName("Cuerpo grande sin tamaño declarado")
	void cuerpoGrandeSinTamanoDeclarado() throws Exception {
		assertError(enviarChunked("/evaluar", GRANDE), 413, "cuerpo-demasiado-grande");
	}

	@Test
	@DisplayName("Cuerpo chico sin tamaño declarado")
	void cuerpoChicoSinTamanoDeclarado() throws Exception {
		byte[] base = JSON.writeValueAsString(PruebaHttp.base()).getBytes(StandardCharsets.UTF_8);
		HttpResponse<String> r = enviarChunked("/evaluar", base);
		assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
		JsonNode json = JSON.readTree(r.body());
		assertThat(json.path("prioridad").asString()).isEqualTo("ALTA");
		assertThat(json.path("score").asInt()).isEqualTo(85);
	}

	@Test
	@DisplayName("Cuerpo grande sin tamaño declarado y JSON inválido")
	void cuerpoGrandeSinTamanoDeclaradoYJsonInvalido() throws Exception {
		assertError(enviarChunked("/evaluar", GRANDE_INVALIDO), 413, "cuerpo-demasiado-grande");
	}

	@Test
	@DisplayName("Cuerpo grande sin tamaño declarado a una ruta desconocida")
	void cuerpoGrandeSinTamanoDeclaradoAUnaRutaDesconocida() throws Exception {
		assertError(enviarChunked("/no-existe", GRANDE), 404, "no-encontrado");
	}

	/** Jackson para al cerrar el objeto: el resto se lee igual y cuenta para el límite. */
	@Test
	void jsonChicoSeguidoDeRellenoQuePasaElLimite() throws Exception {
		byte[] cuerpo = (JSON.writeValueAsString(PruebaHttp.base()) + "x".repeat(20480))
				.getBytes(StandardCharsets.UTF_8);
		assertError(enviarChunked("/evaluar", cuerpo), 413, "cuerpo-demasiado-grande");
	}

	/** POST con la clave válida y un cuerpo de largo desconocido, que el cliente manda chunked. */
	private HttpResponse<String> enviarChunked(String ruta, byte[] cuerpo) throws Exception {
		BodyPublisher publicador = BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(cuerpo));
		assertThat(publicador.contentLength()).as("el pedido no lleva Content-Length").isNegative();
		HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
				.header("X-API-Key", clave)
				.header("Content-Type", "application/json")
				.POST(publicador)
				.build();
		assertThat(pedido.headers().firstValue("Content-Length")).isEmpty();
		return cliente.send(pedido, BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private static void assertError(HttpResponse<String> r, int status, String caso) {
		assertThat(r.statusCode()).as(r.body()).isEqualTo(status);
		JsonNode json = JSON.readTree(r.body());
		assertThat(json.path("type").asString()).isEqualTo(PruebaHttp.URN + caso);
		assertThat(json.path("status").asInt()).isEqualTo(status);
		assertThat(json.has("errores")).isFalse();
	}
}
