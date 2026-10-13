package com.sagant.leads.leads_api.evaluacion.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sagant.leads.leads_api.evaluacion.enums.Prioridad;
import com.sagant.leads.leads_api.evaluacion.enums.TipoMotivo;
import com.sagant.leads.leads_api.evaluacion.model.Evaluacion;
import com.sagant.leads.leads_api.evaluacion.model.Motivo;

/**
 * Convierte los motivos emitidos por las reglas en la evaluación final: verifica que haya
 * exactamente un motivo por regla de negocio, los ordena, suma los de puntaje y aplica la
 * precedencia: si la solicitud no es elegible, score 0 y prioridad BAJA con el motivo de ajuste;
 * si no, recorte a [0, 100] y prioridad por umbrales. Es el único lugar donde se decide esa
 * precedencia. Si cambia esta lógica, hay que subir {@code REVISION_LOGICA} en la versión de reglas.
 */
public class CalculoEvaluacion {

	/** Reglas excluyentes, que van primero en {@code motivos}. */
	public static final List<String> REGLAS_EXCLUYENTES = List.of("edad");

	/** Orden fijo de las reglas de puntaje en {@code motivos}, después de las excluyentes. */
	public static final List<String> ORDEN_REGLAS = List.of("urgencia", "documentacion", "destino",
			"situacion-laboral", "antiguedad", "clearing", "carga");

	private static final List<String> TODAS_LAS_REGLAS = Stream
			.concat(REGLAS_EXCLUYENTES.stream(), ORDEN_REGLAS.stream())
			.toList();

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
				.sorted(Comparator.comparingInt(m -> TODAS_LAS_REGLAS.indexOf(m.regla())))
				.toList();
		int suma = ordenados.stream()
				.filter(m -> m.tipo() == TipoMotivo.PUNTAJE)
				.mapToInt(Motivo::puntos)
				.sum();
		List<String> motivos = new ArrayList<>(ordenados.stream().map(redaccion::texto).toList());
		if (ordenados.stream().anyMatch(Motivo::esNoElegible)) {
			motivos.add(redaccion.ajustePorInelegibilidad(suma));
			return new Evaluacion(Prioridad.BAJA, SCORE_MINIMO, motivos);
		}
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
		Map<String, Long> esperado = TODAS_LAS_REGLAS.stream()
				.collect(Collectors.toMap(Function.identity(), r -> 1L));
		if (!porRegla.equals(esperado)) {
			throw new IllegalStateException("Se esperaba un motivo por regla de negocio " + TODAS_LAS_REGLAS
					+ " y las reglas emitieron " + porRegla);
		}
	}
}
