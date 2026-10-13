package com.sagant.leads.leads_api.evaluacion.dominio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Convierte los motivos emitidos por las reglas en la evaluación final: verifica que haya
 * exactamente un motivo por regla de negocio, los ordena, suma, recorta a [0, 100] y asigna la
 * prioridad. Si cambia esta lógica, hay que subir {@code REVISION_LOGICA} en la versión de reglas.
 */
public class CalculoEvaluacion {

	/** Orden fijo de las reglas de puntaje en {@code motivos}. */
	public static final List<String> ORDEN_REGLAS = List.of("urgencia", "documentacion", "destino",
			"situacion-laboral", "antiguedad", "clearing", "carga");

	private static final int SCORE_MINIMO = 0;
	private static final int SCORE_MAXIMO = 100;

	private final int umbralAlta;
	private final int umbralMedia;
	private final RedaccionMotivos redaccion;

	public CalculoEvaluacion(int umbralAlta, int umbralMedia, RedaccionMotivos redaccion) {
		this.umbralAlta = umbralAlta;
		this.umbralMedia = umbralMedia;
		this.redaccion = redaccion;
	}

	public Evaluacion calcular(List<Motivo> emitidos) {
		verificarUnoPorRegla(emitidos);
		List<Motivo> ordenados = emitidos.stream()
				.sorted(Comparator.comparingInt(m -> ORDEN_REGLAS.indexOf(m.regla())))
				.toList();
		int suma = ordenados.stream().mapToInt(Motivo::puntos).sum();
		List<String> motivos = new ArrayList<>(ordenados.stream().map(redaccion::texto).toList());
		int score = suma;
		if (suma < SCORE_MINIMO) {
			score = SCORE_MINIMO;
			motivos.add(redaccion.recorteAlMinimo(suma));
		}
		else if (suma > SCORE_MAXIMO) {
			score = SCORE_MAXIMO;
			motivos.add(redaccion.recorteAlMaximo(suma));
		}
		return new Evaluacion(prioridad(score), score, motivos);
	}

	private Prioridad prioridad(int score) {
		if (score >= umbralAlta) {
			return Prioridad.ALTA;
		}
		return score >= umbralMedia ? Prioridad.MEDIA : Prioridad.BAJA;
	}

	/** Un motivo de más o de menos es un error del DRL, nunca una respuesta incompleta. */
	private static void verificarUnoPorRegla(List<Motivo> emitidos) {
		Map<String, Long> porRegla = emitidos.stream()
				.collect(Collectors.groupingBy(Motivo::regla, Collectors.counting()));
		Map<String, Long> esperado = ORDEN_REGLAS.stream()
				.collect(Collectors.toMap(Function.identity(), r -> 1L));
		if (!porRegla.equals(esperado)) {
			throw new IllegalStateException("Se esperaba un motivo por regla de negocio " + ORDEN_REGLAS
					+ " y las reglas emitieron " + porRegla);
		}
	}
}
