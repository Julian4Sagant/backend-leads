package com.sagant.leads.leads_api.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

/**
 * La clave de API validada al arrancar. Solo guarda su SHA-256: compara el hash de la clave
 * recibida con {@link MessageDigest#isEqual}, en tiempo constante y sin filtrar el largo.
 *
 * <p>Si la clave falta, tiene menos de 32 caracteres o contiene algo distinto de ASCII imprimible
 * sin espacios, la aplicación no arranca. Los mensajes nunca incluyen la clave.
 */
@Component
public class ClaveApi {

	static final int LARGO_MINIMO = 32;

	private final byte[] hash;

	public ClaveApi(SeguridadProperties propiedades) {
		String clave = propiedades.apiKey();
		if (clave == null || clave.isEmpty()) {
			throw new IllegalStateException("Falta la clave de API: definí la variable de entorno LEADS_API_KEY "
					+ "con al menos " + LARGO_MINIMO + " caracteres.");
		}
		if (clave.length() < LARGO_MINIMO) {
			throw new IllegalStateException("La clave de API (LEADS_API_KEY) debe tener al menos " + LARGO_MINIMO
					+ " caracteres.");
		}
		if (!clave.chars().allMatch(c -> c >= 0x21 && c <= 0x7E)) {
			throw new IllegalStateException("La clave de API (LEADS_API_KEY) solo puede tener caracteres ASCII "
					+ "imprimibles, sin espacios.");
		}
		this.hash = sha256(clave);
	}

	public boolean coincide(String recibida) {
		return recibida != null && MessageDigest.isEqual(hash, sha256(recibida));
	}

	private static byte[] sha256(String texto) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
