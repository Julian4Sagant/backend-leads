package com.sagant.leads.leads_api.evaluacion.dto;

/**
 * Un valor del JSON tal como llegó: el tipo de token y su texto original. Para los números el
 * texto es el del cuerpo (por ejemplo {@code 150000.00} o {@code 1e3}), sin pasar por double.
 */
public record ValorCrudo(Tipo tipo, String texto) {

	public enum Tipo {
		NUMERO, STRING, BOOLEANO, NULO, ARRAY, OBJETO
	}

	public static ValorCrudo numero(String texto) {
		return new ValorCrudo(Tipo.NUMERO, texto);
	}

	public static ValorCrudo string(String texto) {
		return new ValorCrudo(Tipo.STRING, texto);
	}

	public static ValorCrudo de(Tipo tipo) {
		return new ValorCrudo(tipo, null);
	}
}
