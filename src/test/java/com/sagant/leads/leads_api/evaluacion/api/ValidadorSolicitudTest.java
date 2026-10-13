package com.sagant.leads.leads_api.evaluacion.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.assertj.core.groups.Tuple;
import org.springframework.mock.web.MockHttpServletRequest;

import com.sagant.leads.leads_api.evaluacion.api.ValorCrudo.Tipo;
import com.sagant.leads.leads_api.evaluacion.dominio.Clearing;
import com.sagant.leads.leads_api.evaluacion.dominio.Destino;
import com.sagant.leads.leads_api.evaluacion.dominio.Documentacion;
import com.sagant.leads.leads_api.evaluacion.dominio.SituacionLaboral;
import com.sagant.leads.leads_api.evaluacion.dominio.Solicitud;
import com.sagant.leads.leads_api.evaluacion.dominio.Urgencia;

/** Tabla de casos de cada código de error en cada tipo de campo, con los bordes de la spec. */
class ValidadorSolicitudTest {

	private final ValidadorSolicitud validador = new ValidadorSolicitud();

	/** Solicitud base de la spec como valores crudos (números como número JSON). */
	private static Map<String, ValorCrudo> base() {
		Map<String, ValorCrudo> campos = new HashMap<>();
		campos.put("session_id", ValorCrudo.string("beb55e7eb0c947ff86f19aa0e0d0b2db"));
		campos.put("edad", ValorCrudo.numero("32"));
		campos.put("motivo", ValorCrudo.string("auto"));
		campos.put("monto", ValorCrudo.numero("150000"));
		campos.put("plazo_meses", ValorCrudo.numero("24"));
		campos.put("urgencia", ValorCrudo.string("esta_semana"));
		campos.put("situacion_laboral", ValorCrudo.string("dependiente"));
		campos.put("antiguedad_meses", ValorCrudo.numero("36"));
		campos.put("ingreso_mensual", ValorCrudo.numero("60000"));
		campos.put("cuota_deudas", ValorCrudo.numero("5000"));
		campos.put("clearing", ValorCrudo.string("no"));
		campos.put("documentacion", ValorCrudo.string("si"));
		return campos;
	}

	@Test
	void solicitudBaseValida() {
		ResultadoValidacion resultado = validador.validar(new SolicitudCruda(true, base()));
		assertThat(resultado.errores()).isEmpty();
		Solicitud s = resultado.solicitud().orElseThrow();
		assertThat(s.sessionId()).isEqualTo("beb55e7eb0c947ff86f19aa0e0d0b2db");
		assertThat(s.edad()).isEqualTo(32);
		assertThat(s.destino()).isEqualTo(Destino.AUTO);
		assertThat(s.monto()).isEqualByComparingTo("150000");
		assertThat(s.plazoMeses()).isEqualTo(24);
		assertThat(s.urgencia()).isEqualTo(Urgencia.ESTA_SEMANA);
		assertThat(s.situacionLaboral()).isEqualTo(SituacionLaboral.DEPENDIENTE);
		assertThat(s.antiguedadMeses()).isEqualTo(36);
		assertThat(s.ingresoMensual()).isEqualByComparingTo("60000");
		assertThat(s.cuotaDeudas()).isEqualByComparingTo("5000");
		assertThat(s.clearing()).isEqualTo(Clearing.NO);
		assertThat(s.documentacion()).isEqualTo(Documentacion.SI);
		assertThat(resultado.sessionIdValido()).contains("beb55e7eb0c947ff86f19aa0e0d0b2db");
	}

	/**
	 * {campo, tipo de token, texto, código esperado}. {@code OK} significa que la solicitud es válida.
	 * Un texto vacío se escribe entre comillas simples.
	 */
	@ParameterizedTest(name = "{0} = {1} [{2}] -> {3}")
	@CsvSource(delimiter = '|', nullValues = "NULL", value = {
			// Requerido
			"edad|NULO|NULL|requerido",
			"motivo|NULO|NULL|requerido",
			"session_id|NULO|NULL|requerido",
			// Tipo
			"monto|BOOLEANO|NULL|tipo-invalido",
			"plazo_meses|ARRAY|NULL|tipo-invalido",
			"edad|OBJETO|NULL|tipo-invalido",
			"urgencia|NUMERO|5|tipo-invalido",
			"session_id|NUMERO|123|tipo-invalido",
			"clearing|BOOLEANO|NULL|tipo-invalido",
			// Formato de números
			"monto|STRING|150000|OK",
			"monto|NUMERO|150000.00|OK",
			"monto|STRING|abc|formato-invalido",
			"monto|STRING|''|formato-invalido",
			"monto|NUMERO|200.555|formato-invalido",
			"ingreso_mensual|NUMERO|999999999999.99|OK",
			"ingreso_mensual|NUMERO|1000000000000|formato-invalido",
			"cuota_deudas|STRING|0.001|formato-invalido",
			"plazo_meses|NUMERO|12.5|formato-invalido",
			"plazo_meses|STRING|12.5|formato-invalido",
			"edad|NUMERO|32.0|formato-invalido",
			"antiguedad_meses|STRING|x|formato-invalido",
			// Valores permitidos
			"motivo|STRING|otro|OK",
			"urgencia|STRING|1_a_3_meses|OK",
			"urgencia|STRING|pronto|valor-no-permitido",
			"motivo|STRING|Auto|valor-no-permitido",
			"motivo|STRING|' auto'|valor-no-permitido",
			"situacion_laboral|STRING|jubilada|valor-no-permitido",
			"clearing|STRING|No|valor-no-permitido",
			"documentacion|STRING|''|valor-no-permitido",
			// Rangos, con el borde exacto
			"monto|NUMERO|0|fuera-de-rango",
			"monto|NUMERO|0.01|OK",
			"monto|NUMERO|-5|fuera-de-rango",
			"ingreso_mensual|NUMERO|-0.01|fuera-de-rango",
			"ingreso_mensual|NUMERO|0|OK",
			"cuota_deudas|NUMERO|-0.01|fuera-de-rango",
			"cuota_deudas|NUMERO|0|OK",
			"plazo_meses|NUMERO|0|fuera-de-rango",
			"plazo_meses|NUMERO|1|OK",
			"plazo_meses|NUMERO|120|OK",
			"plazo_meses|NUMERO|121|fuera-de-rango",
			"edad|NUMERO|13|fuera-de-rango",
			"edad|NUMERO|14|OK",
			"edad|NUMERO|100|OK",
			"edad|NUMERO|101|fuera-de-rango",
			"antiguedad_meses|NUMERO|-1|fuera-de-rango",
			"antiguedad_meses|NUMERO|0|OK",
			"antiguedad_meses|NUMERO|600|OK",
			"antiguedad_meses|NUMERO|601|fuera-de-rango",
			// session_id
			"session_id|STRING|abc-DEF_123|OK",
			"session_id|STRING|''|formato-invalido",
			"session_id|STRING|sesión1|formato-invalido",
			"session_id|STRING|abc def|formato-invalido",
	})
	void unCampo(String campo, Tipo tipo, String texto, String codigo) {
		Map<String, ValorCrudo> campos = base();
		campos.put(campo, new ValorCrudo(tipo, texto));
		ResultadoValidacion resultado = validador.validar(new SolicitudCruda(true, campos));
		if (codigo.equals("OK")) {
			assertThat(resultado.errores()).isEmpty();
		}
		else {
			assertThat(resultado.errores()).singleElement().satisfies(e -> {
				assertThat(e.campo()).isEqualTo(campo);
				assertThat(e.codigo()).isEqualTo(codigo);
				assertThat(e.mensaje()).isNotBlank();
			});
		}
	}

	@Test
	void sessionIdDe64Y65Caracteres() {
		Map<String, ValorCrudo> campos = base();
		campos.put("session_id", ValorCrudo.string("a".repeat(64)));
		assertThat(validador.validar(new SolicitudCruda(true, campos)).errores()).isEmpty();
		campos.put("session_id", ValorCrudo.string("a".repeat(65)));
		assertThat(validador.validar(new SolicitudCruda(true, campos)).errores())
				.extracting(ErrorCampo::codigo).containsExactly("formato-invalido");
	}

	@Test
	void campoAusenteEsRequerido() {
		Map<String, ValorCrudo> campos = base();
		campos.remove("edad");
		assertThat(validador.validar(new SolicitudCruda(true, campos)).errores())
				.containsExactly(new ErrorCampo("edad", "requerido", "es requerido"));
	}

	@Test
	void objetoVacioDaDoceRequeridosEnOrdenDeCampo() {
		ResultadoValidacion resultado = validador.validar(new SolicitudCruda(true, Map.of()));
		assertThat(resultado.errores()).extracting(ErrorCampo::campo).containsExactlyElementsOf(SolicitudCruda.CAMPOS);
		assertThat(resultado.errores()).extracting(ErrorCampo::codigo).containsOnly("requerido");
	}

	@Test
	void todosLosErroresJuntosEnOrdenDeCampo() {
		Map<String, ValorCrudo> campos = base();
		campos.put("monto", ValorCrudo.de(Tipo.BOOLEANO));
		campos.put("plazo_meses", ValorCrudo.numero("12.5"));
		campos.put("clearing", ValorCrudo.string("talvez"));
		campos.put("edad", ValorCrudo.numero("101"));
		campos.remove("urgencia");
		assertThat(validador.validar(new SolicitudCruda(true, campos)).errores())
				.extracting(ErrorCampo::campo, ErrorCampo::codigo)
				.containsExactly(
						Tuple.tuple("edad", "fuera-de-rango"),
						Tuple.tuple("monto", "tipo-invalido"),
						Tuple.tuple("plazo_meses", "formato-invalido"),
						Tuple.tuple("urgencia", "requerido"),
						Tuple.tuple("clearing", "valor-no-permitido"));
	}

	@Test
	void raizQueNoEsObjeto() {
		assertThat(validador.validar(SolicitudCruda.noEsObjeto()).errores())
				.extracting(ErrorCampo::campo, ErrorCampo::codigo)
				.containsExactly(Tuple.tuple("$", "tipo-invalido"));
	}

	@Test
	void unImporteComoStringYComoNumeroDanElMismoValor() {
		Map<String, ValorCrudo> comoString = base();
		comoString.put("monto", ValorCrudo.string("150000"));
		BigDecimal desdeString = validador.validar(new SolicitudCruda(true, comoString)).solicitud().orElseThrow().monto();
		BigDecimal desdeNumero = validador.validar(new SolicitudCruda(true, base())).solicitud().orElseThrow().monto();
		assertThat(desdeString).isEqualByComparingTo(desdeNumero);
	}

	// ---- 5.3: session_id en el request ----

	@Test
	void sessionIdConSaltoDeLineaNoSeGuarda() {
		Map<String, ValorCrudo> campos = base();
		campos.put("session_id", ValorCrudo.string("abc\ndef"));
		assertThat(guardadoEnElRequest(campos)).isNull();
	}

	@ParameterizedTest(name = "session_id inválido no se guarda: [{0}]")
	@CsvSource(delimiter = '|', value = { "''", "sesión1", "abc def" })
	void sessionIdInvalidoNoSeGuarda(String sessionId) {
		Map<String, ValorCrudo> campos = base();
		campos.put("session_id", ValorCrudo.string(sessionId));
		assertThat(guardadoEnElRequest(campos)).isNull();
	}

	@Test
	void sessionIdDe65CaracteresNoSeGuarda() {
		Map<String, ValorCrudo> campos = base();
		campos.put("session_id", ValorCrudo.string("a".repeat(65)));
		assertThat(guardadoEnElRequest(campos)).isNull();
	}

	@Test
	void sessionIdValidoSeGuardaAunqueOtroCampoTengaErrores() {
		Map<String, ValorCrudo> campos = base();
		campos.put("edad", ValorCrudo.numero("101"));
		assertThat(guardadoEnElRequest(campos)).isEqualTo("beb55e7eb0c947ff86f19aa0e0d0b2db");
	}

	private Object guardadoEnElRequest(Map<String, ValorCrudo> campos) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		SessionIdDelRequest.guardar(request, validador.validar(new SolicitudCruda(true, campos)));
		return SessionIdDelRequest.leer(request).orElse(null);
	}
}
