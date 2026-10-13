package com.sagant.leads.leads_api.evaluacion.api;

/**
 * Un error de validación de un campo. {@code codigo} es parte del contrato; {@code mensaje} no:
 * n8n decide solo por {@code type} y {@code codigo}. El mensaje nunca incluye el valor recibido.
 */
public record ErrorCampo(String campo, String codigo, String mensaje) {

	public static final String REQUERIDO = "requerido";
	public static final String TIPO_INVALIDO = "tipo-invalido";
	public static final String FORMATO_INVALIDO = "formato-invalido";
	public static final String VALOR_NO_PERMITIDO = "valor-no-permitido";
	public static final String FUERA_DE_RANGO = "fuera-de-rango";

	/** Campo usado cuando el cuerpo es JSON válido pero no un objeto. */
	public static final String RAIZ = "$";
}
