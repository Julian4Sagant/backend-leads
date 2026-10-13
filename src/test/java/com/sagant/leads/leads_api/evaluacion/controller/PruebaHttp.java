package com.sagant.leads.leads_api.evaluacion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base de los tests HTTP: la aplicación real con MockMvc y una clave ficticia. Las respuestas se
 * leen siempre como bytes UTF-8 (MockMvc decodifica en ISO-8859-1 si no hay charset).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PruebaHttp {

	public static final String URN = "urn:leads-api:error:";
	public static final String SESSION_ID_BASE = "beb55e7eb0c947ff86f19aa0e0d0b2db";

	/** Marca un campo que se quita de la solicitud base. */
	public static final Object SIN = new Object();

	protected static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	protected MockMvc mvc;

	@Value("${leads.seguridad.api-key}")
	protected String clave;

	/** Solicitud base de la spec {@code evaluacion-leads}: score 85, ALTA. Datos ficticios. */
	public static Map<String, Object> base() {
		Map<String, Object> s = new LinkedHashMap<>();
		s.put("session_id", SESSION_ID_BASE);
		s.put("edad", 32);
		s.put("motivo", "auto");
		s.put("monto", 150000);
		s.put("plazo_meses", 24);
		s.put("urgencia", "esta_semana");
		s.put("situacion_laboral", "dependiente");
		s.put("antiguedad_meses", 36);
		s.put("ingreso_mensual", 60000);
		s.put("cuota_deudas", 5000);
		s.put("clearing", "no");
		s.put("documentacion", "si");
		return s;
	}

	/** La solicitud base con cambios: pares campo, valor; {@link #SIN} quita el campo. */
	public static Map<String, Object> con(Object... cambios) {
		Map<String, Object> s = base();
		for (int i = 0; i < cambios.length; i += 2) {
			if (cambios[i + 1] == SIN) {
				s.remove((String) cambios[i]);
			}
			else {
				s.put((String) cambios[i], cambios[i + 1]);
			}
		}
		return s;
	}

	/** POST /evaluar con la clave válida y Content-Type JSON. */
	protected Respuesta evaluar(Map<String, Object> cuerpo) throws Exception {
		return evaluarCrudo(JSON.writeValueAsString(cuerpo));
	}

	protected Respuesta evaluarCrudo(String cuerpo) throws Exception {
		return enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo.getBytes(StandardCharsets.UTF_8)));
	}

	/** Cuerpo JSON con un número sin comillas, escrito tal cual (por ejemplo {@code 150000.00}). */
	protected Respuesta evaluarConNumero(String campo, String numero) throws Exception {
		Map<String, Object> s = base();
		s.put(campo, "@@NUMERO@@");
		return evaluarCrudo(JSON.writeValueAsString(s).replace("\"@@NUMERO@@\"", numero));
	}

	protected Respuesta enviar(MockHttpServletRequestBuilder peticion) throws Exception {
		MvcResult resultado = mvc.perform(peticion).andReturn();
		byte[] bytes = resultado.getResponse().getContentAsByteArray();
		String cuerpo = new String(bytes, StandardCharsets.UTF_8);
		JsonNode json = cuerpo.isBlank() ? null : JSON.readTree(cuerpo);
		return new Respuesta(resultado.getResponse().getStatus(), resultado.getResponse().getContentType(), bytes,
				cuerpo, json);
	}

	public record Respuesta(int status, String contentType, byte[] bytes, String cuerpo, JsonNode json) {

		public String type() {
			return json.path("type").asString();
		}

		public List<String> motivos() {
			List<String> motivos = new ArrayList<>();
			json.path("motivos").forEach(m -> motivos.add(m.asString()));
			return motivos;
		}

		public int score() {
			return json.path("score").asInt();
		}

		public String prioridad() {
			return json.path("prioridad").asString();
		}

		/** Pares campo:codigo de {@code errores}, en orden. */
		public List<String> errores() {
			List<String> errores = new ArrayList<>();
			json.path("errores").forEach(e -> errores.add(e.path("campo").asString() + ":" + e.path("codigo").asString()));
			return errores;
		}
	}

	// ---- Aserciones comunes ----

	protected static void assertOk(Respuesta r) {
		assertThat(r.status()).as(r.cuerpo()).isEqualTo(200);
	}

	protected static void assertEvaluacion(Respuesta r, int score, String prioridad) {
		assertOk(r);
		assertThat(r.score()).isEqualTo(score);
		assertThat(r.prioridad()).isEqualTo(prioridad);
	}

	protected static void assertIncluyeMotivo(Respuesta r, String motivo) {
		assertOk(r);
		assertThat(r.motivos()).contains(motivo);
	}

	/** 400 {@code validacion} con un error {@code codigo} en {@code campo}. */
	protected static void assertValidacion(Respuesta r, String campo, String codigo) {
		assertThat(r.status()).as(r.cuerpo()).isEqualTo(400);
		assertThat(r.type()).isEqualTo(URN + "validacion");
		assertThat(r.errores()).contains(campo + ":" + codigo);
	}

	protected static void assertError(Respuesta r, int status, String caso) {
		assertThat(r.status()).as(r.cuerpo()).isEqualTo(status);
		assertThat(r.type()).isEqualTo(URN + caso);
		assertThat(r.json().path("status").asInt()).isEqualTo(status);
	}

	/** Content-Type con su charset, sin importar mayúsculas ni espacios. */
	protected static void assertContentType(Respuesta r, String esperado) {
		assertThat(MediaType.parseMediaType(r.contentType())).isEqualTo(MediaType.parseMediaType(esperado));
	}
}
