package com.sagant.leads.leads_api.evaluacion.service;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;
import com.sagant.leads.leads_api.evaluacion.model.Motivo;

/**
 * Textos en español de los motivos. El DRL emite solo códigos (es ASCII); acá se arman las frases
 * de la spec.
 *
 * <p>Los textos de antigüedad y carga usan los umbrales configurados, para que un motivo nunca
 * describa un corte distinto del real. Si cambia un texto o la forma de armarlo, hay que subir
 * {@code REVISION_LOGICA} en la versión de reglas.
 */
@Component
public class TextosMotivos implements RedaccionMotivos {

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private static final Map<String, String> FIJOS = Map.ofEntries(
			Map.entry("urgencia/esta-semana", "Urgencia esta semana"),
			Map.entry("urgencia/este-mes", "Urgencia este mes"),
			Map.entry("urgencia/1-a-3-meses", "Urgencia de 1 a 3 meses"),
			Map.entry("urgencia/sin-apuro", "Sin urgencia"),
			Map.entry("documentacion/si", "Documentación de ingresos disponible"),
			Map.entry("documentacion/no", "Sin documentación de ingresos"),
			Map.entry("destino/definido", "Destino del préstamo definido"),
			Map.entry("destino/no-definido", "Destino del préstamo no definido"),
			Map.entry("situacion-laboral/dependiente", "Empleado en relación de dependencia"),
			Map.entry("situacion-laboral/independiente", "Trabajador independiente"),
			Map.entry("situacion-laboral/jubilado", "Jubilado"),
			Map.entry("situacion-laboral/estudiante", "Estudiante"),
			Map.entry("situacion-laboral/desempleado", "Desempleado"),
			Map.entry("clearing/si", "Figura en el Clearing de Informes"),
			Map.entry("clearing/no-se", "No sabe si figura en el Clearing de Informes"),
			Map.entry("clearing/no", "No figura en el Clearing de Informes"));

	private static final String NO_APLICA = "no-aplica-";

	private final ReglasProperties.Umbrales umbrales;

	public TextosMotivos(ReglasProperties reglas) {
		this.umbrales = reglas.umbrales();
	}

	@Override
	public String texto(Motivo motivo) {
		return frase(motivo) + ": " + puntaje(motivo.puntos());
	}

	@Override
	public String recorteAlMinimo(int suma) {
		return "Score limitado al mínimo de 0 (suma " + suma + ")";
	}

	@Override
	public String recorteAlMaximo(int suma) {
		return "Score limitado al máximo de 100 (suma " + suma + ")";
	}

	/** {@code +N} si es positivo, {@code -N} si es negativo (guion ASCII) y {@code 0} sin signo. */
	static String puntaje(int puntos) {
		return puntos > 0 ? "+" + puntos : Integer.toString(puntos);
	}

	private String frase(Motivo motivo) {
		String clave = motivo.regla() + "/" + motivo.codigo();
		String fija = FIJOS.get(clave);
		if (fija != null) {
			return fija;
		}
		return switch (clave) {
			case "antiguedad/alta" -> "Antigüedad laboral de " + numero(umbrales.antiguedad().mesesAlto()) + " meses o más";
			case "antiguedad/media" -> "Antigüedad laboral de " + numero(umbrales.antiguedad().mesesMedio()) + " a "
					+ numero(umbrales.antiguedad().mesesAlto().subtract(BigDecimal.ONE)) + " meses";
			case "antiguedad/baja" -> "Antigüedad laboral de menos de " + numero(umbrales.antiguedad().mesesMedio()) + " meses";
			case "carga/baja" -> "Carga de deuda hasta el " + porcentaje(umbrales.carga().baja()) + "% del ingreso";
			case "carga/media" -> "Carga de deuda de más del " + porcentaje(umbrales.carga().baja()) + "% y hasta el "
					+ porcentaje(umbrales.carga().alta()) + "% del ingreso";
			case "carga/alta" -> "Carga de deuda de más del " + porcentaje(umbrales.carga().alta()) + "% del ingreso";
			default -> {
				if (motivo.regla().equals("antiguedad") && motivo.codigo().startsWith(NO_APLICA)) {
					yield "Antigüedad laboral no aplica a " + motivo.codigo().substring(NO_APLICA.length());
				}
				throw new IllegalStateException("Motivo sin texto: " + clave);
			}
		};
	}

	private static String numero(BigDecimal valor) {
		return valor.stripTrailingZeros().toPlainString();
	}

	private static String porcentaje(BigDecimal fraccion) {
		return numero(fraccion.multiply(CIEN));
	}
}
