package com.sagant.leads.leads_api.evaluacion.reglas;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;

/**
 * Versión de las reglas que acompaña cada respuesta: los primeros 12 caracteres hexadecimales del
 * SHA-256 de la configuración canónica, el DRL y la revisión de la lógica en Java.
 *
 * <p>La configuración ({@code prioridad}, {@code umbrales} y {@code puntajes}) se aplana a líneas
 * {@code clave=valor} ordenadas por clave, con cada número normalizado para que {@code 20} y
 * {@code 20.0} den lo mismo. La ruta del archivo no entra: cambia dónde está el DRL, no la lógica.
 * El DRL entra con los finales de línea normalizados a LF.
 */
public record VersionReglas(String valor) {

	/**
	 * Revisión de la lógica que vive en Java (textos de motivos, orden, cálculo de score y
	 * prioridad). Subirla cada vez que esa lógica cambia, y también cuando cambia la lista de
	 * reglas del DRL (lo vigila el test de cierre).
	 */
	public static final int REVISION_LOGICA = 1;

	private static final int LARGO = 12;

	public static VersionReglas calcular(ReglasProperties reglas, String drl) {
		return calcular(reglas, drl, REVISION_LOGICA);
	}

	static VersionReglas calcular(ReglasProperties reglas, String drl, int revisionLogica) {
		StringBuilder canonico = new StringBuilder();
		Map<String, String> lineas = new TreeMap<>();
		aplanar("prioridad", reglas.prioridad(), lineas);
		aplanar("umbrales", reglas.umbrales(), lineas);
		aplanar("puntajes", reglas.puntajes(), lineas);
		lineas.forEach((clave, valor) -> canonico.append(clave).append('=').append(valor).append('\n'));
		canonico.append("--- drl ---\n").append(drl.replace("\r\n", "\n"));
		canonico.append("\n--- revision ---\n").append(revisionLogica);
		return new VersionReglas(HexFormat.of().formatHex(sha256(canonico.toString())).substring(0, LARGO));
	}

	private static void aplanar(String prefijo, Object valor, Map<String, String> lineas) {
		if (valor instanceof Record registro) {
			for (RecordComponent componente : registro.getClass().getRecordComponents()) {
				aplanar(prefijo + "." + componente.getName(), leer(componente, registro), lineas);
			}
		}
		else if (valor instanceof BigDecimal numero) {
			lineas.put(prefijo, numero.stripTrailingZeros().toPlainString());
		}
		else {
			lineas.put(prefijo, String.valueOf(valor));
		}
	}

	private static Object leer(RecordComponent componente, Record registro) {
		try {
			return componente.getAccessor().invoke(registro);
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No se pudo leer " + componente.getName(), e);
		}
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
