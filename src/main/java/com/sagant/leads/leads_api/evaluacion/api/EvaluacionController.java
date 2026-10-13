package com.sagant.leads.leads_api.evaluacion.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sagant.leads.leads_api.comun.error.ValidacionException;
import com.sagant.leads.leads_api.evaluacion.dominio.Evaluacion;
import com.sagant.leads.leads_api.evaluacion.dominio.Solicitud;
import com.sagant.leads.leads_api.evaluacion.reglas.Evaluador;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class EvaluacionController {

	static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");

	private static final Logger log = LoggerFactory.getLogger(EvaluacionController.class);

	private final ValidadorSolicitud validador;
	private final Evaluador evaluador;

	public EvaluacionController(ValidadorSolicitud validador, Evaluador evaluador) {
		this.validador = validador;
		this.evaluador = evaluador;
	}

	@PostMapping(path = "/evaluar", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<RespuestaEvaluacion> evaluar(@RequestBody SolicitudCruda cuerpo, HttpServletRequest request) {
		ResultadoValidacion resultado = validador.validar(cuerpo);
		SessionIdDelRequest.guardar(request, resultado);
		Solicitud solicitud = resultado.solicitud().orElseThrow(() -> new ValidacionException(resultado.errores()));

		Evaluacion evaluacion = evaluador.evaluar(solicitud);
		String version = evaluador.version();
		log.info("evaluacion session_id={} prioridad={} score={} version_reglas={}", solicitud.sessionId(),
				evaluacion.prioridad(), evaluacion.score(), version);
		return ResponseEntity.ok().contentType(JSON_UTF8).body(new RespuestaEvaluacion(
				evaluacion.prioridad().name(), evaluacion.score(), evaluacion.motivos(), version));
	}
}
