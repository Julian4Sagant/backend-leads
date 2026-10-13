package com.sagant.leads.leads_api.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

/**
 * Tarea 6.16: qué queda en el log. Se miran los mensajes (lo que sigue a " : " en el formato de
 * Spring Boot), para que el PID o la hora no generen coincidencias falsas.
 */
@ExtendWith(OutputCaptureExtension.class)
class LogsHttpTest extends PruebaHttp {

	@Test
	@DisplayName("Línea de evaluación exitosa")
	void lineaDeEvaluacionExitosa(CapturedOutput salida) throws Exception {
		Respuesta r = evaluar(base());
		assertOk(r);
		String version = r.json().path("version_reglas").asString();
		assertThat(mensajes(salida)).filteredOn(m -> m.startsWith("evaluacion ")).singleElement()
				.isEqualTo("evaluacion session_id=" + SESSION_ID_BASE + " prioridad=ALTA score=85 version_reglas=" + version);
	}

	@Test
	@DisplayName("Sin otros datos de la solicitud")
	void sinOtrosDatosDeLaSolicitud(CapturedOutput salida) throws Exception {
		Respuesta r = evaluarCrudo(JSON.writeValueAsString(base())
				.replace("\"monto\":150000", "\"monto\":123457.89")
				.replace("\"ingreso_mensual\":60000", "\"ingreso_mensual\":98765.43")
				.replace("\"cuota_deudas\":5000", "\"cuota_deudas\":4321.09"));
		assertOk(r);
		assertThat(String.join("\n", mensajes(salida))).doesNotContain("123457").doesNotContain("98765")
				.doesNotContain("4321");
	}

	@Test
	@DisplayName("Error de validación con session_id válido")
	void errorDeValidacionConSessionIdValido(CapturedOutput salida) throws Exception {
		assertThat(evaluar(con("edad", 101)).status()).isEqualTo(400);
		assertThat(mensajes(salida)).filteredOn(m -> m.startsWith("error ")).singleElement()
				.isEqualTo("error type=" + URN + "validacion status=400 session_id=" + SESSION_ID_BASE);
	}

	@Test
	@DisplayName("Error de validación sin valores del cuerpo")
	void errorDeValidacionSinValoresDelCuerpo(CapturedOutput salida) throws Exception {
		Respuesta r = evaluar(con("monto", "valor-secreto-123", "motivo", "otro-secreto-456"));
		assertThat(r.status()).isEqualTo(400);
		String todo = salida.getAll();
		assertThat(todo).doesNotContain("valor-secreto-123").doesNotContain("otro-secreto-456");
		r.json().path("errores").forEach(e -> assertThat(todo).doesNotContain(e.path("mensaje").asString()));
	}

	@Test
	@DisplayName("session_id con salto de línea no se registra")
	void sessionIdConSaltoDeLineaNoSeRegistra(CapturedOutput salida) throws Exception {
		assertThat(evaluar(con("session_id", "abc\ndef")).status()).isEqualTo(400);
		List<String> errores = mensajes(salida).stream().filter(m -> m.startsWith("error ")).toList();
		assertThat(errores).containsExactly("error type=" + URN + "validacion status=400");
		assertThat(lineas(salida)).noneMatch(l -> l.startsWith("def")).noneMatch(l -> l.contains("abc"));
	}

	@Test
	@DisplayName("JSON inválido sin session_id")
	void jsonInvalidoSinSessionId(CapturedOutput salida) throws Exception {
		Respuesta r = evaluarCrudo("{\"session_id\": \"" + SESSION_ID_BASE + "\",");
		assertError(r, 400, "json-invalido");
		assertThat(mensajes(salida)).filteredOn(m -> m.startsWith("error ")).singleElement()
				.isEqualTo("error type=" + URN + "json-invalido status=400");
		assertThat(salida.getAll()).doesNotContain(SESSION_ID_BASE);
	}

	@Test
	@DisplayName("Clave incorrecta no se registra")
	void claveIncorrectaNoSeRegistra(CapturedOutput salida) throws Exception {
		String incorrecta = "clave-incorrecta-de-prueba-0000000000";
		Respuesta r = enviar(post("/evaluar").header("X-API-Key", incorrecta).contentType(MediaType.APPLICATION_JSON)
				.content(JSON.writeValueAsString(base())));
		assertError(r, 401, "no-autorizado");
		assertThat(mensajes(salida)).filteredOn(m -> m.startsWith("error ")).singleElement()
				.isEqualTo("error type=" + URN + "no-autorizado status=401");
		assertThat(salida.getAll()).doesNotContain(incorrecta).doesNotContain(clave);
	}

	private static List<String> lineas(CapturedOutput salida) {
		return Arrays.asList(salida.getAll().split("\\R"));
	}

	/** El mensaje de cada línea de log (lo que sigue a " : "). */
	private static List<String> mensajes(CapturedOutput salida) {
		return lineas(salida).stream().filter(l -> l.contains(" : "))
				.map(l -> l.substring(l.indexOf(" : ") + 3).strip()).toList();
	}
}
