package com.sagant.leads.leads_api.evaluacion.api;

import java.util.HashMap;
import java.util.Map;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Lee el cuerpo sin convertir tipos, para que el validador vea cada valor tal como llegó y pueda
 * informar todos los errores juntos. Jackson nunca falla por un tipo incorrecto: solo por un JSON
 * que no se puede parsear (incluido un número más largo que el límite del parser).
 * Si un campo se repite, gana el último.
 */
public class LectorSolicitudCruda extends ValueDeserializer<SolicitudCruda> {

	@Override
	public SolicitudCruda deserialize(JsonParser parser, DeserializationContext contexto) {
		JsonToken token = parser.currentToken();
		if (token != JsonToken.START_OBJECT && token != JsonToken.PROPERTY_NAME) {
			parser.skipChildren();
			return SolicitudCruda.noEsObjeto();
		}
		Map<String, ValorCrudo> campos = new HashMap<>();
		String nombre = token == JsonToken.PROPERTY_NAME ? parser.currentName() : parser.nextName();
		while (nombre != null) {
			JsonToken valor = parser.nextToken();
			if (SolicitudCruda.CAMPOS.contains(nombre)) {
				campos.put(nombre, crudo(parser, valor));
			}
			else {
				parser.skipChildren();
			}
			nombre = parser.nextName();
		}
		return new SolicitudCruda(true, campos);
	}

	/** El literal {@code null} como cuerpo es JSON válido pero no un objeto. */
	@Override
	public SolicitudCruda getNullValue(DeserializationContext contexto) {
		return SolicitudCruda.noEsObjeto();
	}

	private static ValorCrudo crudo(JsonParser parser, JsonToken token) {
		return switch (token) {
			case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> ValorCrudo.numero(parser.getString());
			case VALUE_STRING -> ValorCrudo.string(parser.getString());
			case VALUE_TRUE, VALUE_FALSE -> ValorCrudo.de(ValorCrudo.Tipo.BOOLEANO);
			case VALUE_NULL -> ValorCrudo.de(ValorCrudo.Tipo.NULO);
			case START_ARRAY -> {
				parser.skipChildren();
				yield ValorCrudo.de(ValorCrudo.Tipo.ARRAY);
			}
			case START_OBJECT -> {
				parser.skipChildren();
				yield ValorCrudo.de(ValorCrudo.Tipo.OBJETO);
			}
			default -> throw new IllegalStateException("Token inesperado: " + token);
		};
	}
}
