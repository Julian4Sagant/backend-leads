package com.sagant.leads.leads_api.http;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tareas 6.4 a 6.7: campos requeridos, valores de código, formato de números, rangos y
 * session_id, de punta a punta por HTTP.
 */
class ContratoHttpTest extends PruebaHttp {

	// ---- 6.4 Requeridos y códigos ----

	@Test
	@DisplayName("Campo ausente")
	void campoAusente() throws Exception {
		assertValidacion(evaluar(con("edad", SIN)), "edad", "requerido");
	}

	@Test
	@DisplayName("Campo null")
	void campoNull() throws Exception {
		assertValidacion(evaluar(con("antiguedad_meses", null)), "antiguedad_meses", "requerido");
	}

	@Test
	@DisplayName("Objeto vacío")
	void objetoVacio() throws Exception {
		Respuesta r = evaluarCrudo("{}");
		assertThat(r.status()).isEqualTo(400);
		assertThat(r.type()).isEqualTo(URN + "validacion");
		assertThat(r.errores()).hasSize(12).allSatisfy(e -> assertThat(e).endsWith(":requerido"));
	}

	@Test
	@DisplayName("Varios errores en una misma solicitud")
	void variosErroresEnUnaMismaSolicitud() throws Exception {
		Respuesta r = evaluar(con("edad", 13, "clearing", "talvez"));
		assertThat(r.status()).isEqualTo(400);
		assertThat(r.errores()).containsExactly("edad:fuera-de-rango", "clearing:valor-no-permitido");
	}

	@Test
	@DisplayName("Código válido")
	void codigoValido() throws Exception {
		assertOk(evaluar(con("urgencia", "1_a_3_meses")));
	}

	@Test
	@DisplayName("Código desconocido")
	void codigoDesconocido() throws Exception {
		assertValidacion(evaluar(con("urgencia", "pronto")), "urgencia", "valor-no-permitido");
	}

	@Test
	@DisplayName("Código con mayúscula")
	void codigoConMayuscula() throws Exception {
		assertValidacion(evaluar(con("motivo", "Auto")), "motivo", "valor-no-permitido");
	}

	@Test
	@DisplayName("Código con espacios")
	void codigoConEspacios() throws Exception {
		assertValidacion(evaluar(con("motivo", " auto")), "motivo", "valor-no-permitido");
	}

	@Test
	@DisplayName("Código con tipo incorrecto")
	void codigoConTipoIncorrecto() throws Exception {
		assertValidacion(evaluar(con("urgencia", 5)), "urgencia", "tipo-invalido");
	}

	// ---- 6.5 Formato de números ----

	@Test
	@DisplayName("Número como string")
	void numeroComoString() throws Exception {
		Respuesta r = evaluar(con("monto", "150000"));
		assertOk(r);
		assertThat(r.cuerpo()).isEqualTo(evaluar(base()).cuerpo());
	}

	@Test
	@DisplayName("Importe con dos decimales")
	void importeConDosDecimales() throws Exception {
		Respuesta r = evaluarConNumero("monto", "150000.00");
		assertOk(r);
		assertThat(r.cuerpo()).isEqualTo(evaluar(base()).cuerpo());
	}

	@Test
	@DisplayName("Importe con doce dígitos enteros")
	void importeConDoceDigitosEnteros() throws Exception {
		assertOk(evaluarConNumero("ingreso_mensual", "999999999999.99"));
	}

	@Test
	@DisplayName("Importe con trece dígitos enteros")
	void importeConTreceDigitosEnteros() throws Exception {
		assertValidacion(evaluarConNumero("ingreso_mensual", "1000000000000"), "ingreso_mensual", "formato-invalido");
	}

	@Test
	@DisplayName("Importe con tres decimales")
	void importeConTresDecimales() throws Exception {
		assertValidacion(evaluarConNumero("monto", "200.555"), "monto", "formato-invalido");
	}

	@Test
	@DisplayName("Entero con parte decimal")
	void enteroConParteDecimal() throws Exception {
		assertValidacion(evaluarConNumero("plazo_meses", "12.5"), "plazo_meses", "formato-invalido");
	}

	@Test
	@DisplayName("Texto no numérico")
	void textoNoNumerico() throws Exception {
		assertValidacion(evaluar(con("monto", "abc")), "monto", "formato-invalido");
	}

	@Test
	@DisplayName("String vacío en un número")
	void stringVacioEnUnNumero() throws Exception {
		assertValidacion(evaluar(con("monto", "")), "monto", "formato-invalido");
	}

	@Test
	@DisplayName("Booleano en un número")
	void booleanoEnUnNumero() throws Exception {
		assertValidacion(evaluar(con("plazo_meses", true)), "plazo_meses", "tipo-invalido");
	}

	@Test
	@DisplayName("Array en un número")
	void arrayEnUnNumero() throws Exception {
		assertValidacion(evaluarConNumero("monto", "[]"), "monto", "tipo-invalido");
	}

	// ---- 6.6 Rangos ----

	@Test
	@DisplayName("Monto cero")
	void montoCero() throws Exception {
		assertValidacion(evaluar(con("monto", 0)), "monto", "fuera-de-rango");
	}

	@Test
	@DisplayName("Monto mínimo positivo")
	void montoMinimoPositivo() throws Exception {
		assertOk(evaluarConNumero("monto", "0.01"));
	}

	@Test
	@DisplayName("Ingreso negativo")
	void ingresoNegativo() throws Exception {
		assertValidacion(evaluarConNumero("ingreso_mensual", "-0.01"), "ingreso_mensual", "fuera-de-rango");
	}

	@Test
	@DisplayName("Ingreso cero")
	void ingresoCero() throws Exception {
		assertOk(evaluar(con("ingreso_mensual", 0)));
	}

	@Test
	@DisplayName("Cuota de deudas negativa")
	void cuotaDeDeudasNegativa() throws Exception {
		assertValidacion(evaluarConNumero("cuota_deudas", "-0.01"), "cuota_deudas", "fuera-de-rango");
	}

	@Test
	@DisplayName("Cuota de deudas cero")
	void cuotaDeDeudasCero() throws Exception {
		assertOk(evaluar(con("cuota_deudas", 0)));
	}

	@Test
	@DisplayName("Plazo 0")
	void plazo0() throws Exception {
		assertValidacion(evaluar(con("plazo_meses", 0)), "plazo_meses", "fuera-de-rango");
	}

	@Test
	@DisplayName("Plazo 1")
	void plazo1() throws Exception {
		assertOk(evaluar(con("plazo_meses", 1)));
	}

	@Test
	@DisplayName("Plazo 120")
	void plazo120() throws Exception {
		assertOk(evaluar(con("plazo_meses", 120)));
	}

	@Test
	@DisplayName("Plazo 121")
	void plazo121() throws Exception {
		assertValidacion(evaluar(con("plazo_meses", 121)), "plazo_meses", "fuera-de-rango");
	}

	@Test
	@DisplayName("Edad 13")
	void edad13() throws Exception {
		assertValidacion(evaluar(con("edad", 13)), "edad", "fuera-de-rango");
	}

	@Test
	@DisplayName("Edad 14")
	void edad14() throws Exception {
		assertOk(evaluar(con("edad", 14)));
	}

	@Test
	@DisplayName("Edad 100")
	void edad100() throws Exception {
		assertOk(evaluar(con("edad", 100)));
	}

	@Test
	@DisplayName("Edad 101")
	void edad101() throws Exception {
		assertValidacion(evaluar(con("edad", 101)), "edad", "fuera-de-rango");
	}

	@Test
	@DisplayName("Antigüedad -1")
	void antiguedadMenos1() throws Exception {
		assertValidacion(evaluar(con("antiguedad_meses", -1)), "antiguedad_meses", "fuera-de-rango");
	}

	@Test
	@DisplayName("Antigüedad 0")
	void antiguedad0() throws Exception {
		assertOk(evaluar(con("antiguedad_meses", 0)));
	}

	@Test
	@DisplayName("Antigüedad 600")
	void antiguedad600() throws Exception {
		assertOk(evaluar(con("antiguedad_meses", 600)));
	}

	@Test
	@DisplayName("Antigüedad 601")
	void antiguedad601() throws Exception {
		assertValidacion(evaluar(con("antiguedad_meses", 601)), "antiguedad_meses", "fuera-de-rango");
	}

	// ---- 6.7 session_id ----

	@Test
	@DisplayName("session_id de 64 caracteres")
	void sessionIdDe64Caracteres() throws Exception {
		assertOk(evaluar(con("session_id", "a".repeat(64))));
	}

	@Test
	@DisplayName("session_id de 65 caracteres")
	void sessionIdDe65Caracteres() throws Exception {
		assertValidacion(evaluar(con("session_id", "a".repeat(65))), "session_id", "formato-invalido");
	}

	@Test
	@DisplayName("session_id vacío")
	void sessionIdVacio() throws Exception {
		assertValidacion(evaluar(con("session_id", "")), "session_id", "formato-invalido");
	}

	@Test
	@DisplayName("session_id con carácter no ASCII")
	void sessionIdConCaracterNoAscii() throws Exception {
		assertValidacion(evaluar(con("session_id", "sesión1")), "session_id", "formato-invalido");
	}

	@Test
	@DisplayName("session_id con salto de línea")
	void sessionIdConSaltoDeLinea() throws Exception {
		assertValidacion(evaluar(con("session_id", "abc\ndef")), "session_id", "formato-invalido");
	}
}
