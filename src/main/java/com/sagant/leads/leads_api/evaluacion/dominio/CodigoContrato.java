package com.sagant.leads.leads_api.evaluacion.dominio;

import java.util.Optional;

/**
 * Enum cuyo valor en el contrato con n8n es un código explícito (por ejemplo {@code 1_a_3_meses}),
 * independiente del nombre de la constante. El mapeo es exacto: distingue mayúsculas y no recorta
 * espacios.
 */
public interface CodigoContrato {

	String codigo();

	static <E extends Enum<E> & CodigoContrato> Optional<E> desdeCodigo(Class<E> tipo, String codigo) {
		for (E valor : tipo.getEnumConstants()) {
			if (valor.codigo().equals(codigo)) {
				return Optional.of(valor);
			}
		}
		return Optional.empty();
	}
}
