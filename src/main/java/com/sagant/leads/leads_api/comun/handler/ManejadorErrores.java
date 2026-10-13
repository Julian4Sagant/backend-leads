package com.sagant.leads.leads_api.comun.handler;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sagant.leads.leads_api.comun.enums.TipoError;
import com.sagant.leads.leads_api.comun.exception.NoAutorizadoException;
import com.sagant.leads.leads_api.comun.exception.ValidacionException;
import com.sagant.leads.leads_api.evaluacion.validation.SessionIdDelRequest;

/**
 * Único lugar que arma respuestas de error: ProblemDetail (RFC 9457) con un {@code type} del
 * catálogo y {@code application/problem+json;charset=UTF-8}. También los errores de los filtros
 * llegan acá.
 *
 * <p>Cada error deja una línea de log con el {@code type}, el estado y, si se validó, el
 * {@code session_id}. Nunca se registran valores recibidos ni mensajes de error de campo.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

	static final MediaType PROBLEM_JSON_UTF8 = MediaType.parseMediaType("application/problem+json;charset=UTF-8");

	private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

	@ExceptionHandler(NoAutorizadoException.class)
	ResponseEntity<Object> noAutorizado(NoAutorizadoException ex, WebRequest request) {
		return respuesta(TipoError.NO_AUTORIZADO, new HttpHeaders(), request, null);
	}

	@ExceptionHandler(ValidacionException.class)
	ResponseEntity<Object> validacion(ValidacionException ex, WebRequest request) {
		ProblemDetail problema = problema(TipoError.VALIDACION);
		problema.setProperty("errores", ex.errores());
		return enviar(TipoError.VALIDACION, problema, new HttpHeaders(), request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> interno(Exception ex, WebRequest request) {
		return respuesta(TipoError.INTERNO, new HttpHeaders(), request, ex);
	}

	/** Las excepciones de Spring MVC pasan por acá: se reemplaza su ProblemDetail por el del catálogo. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		TipoError tipo = switch (ex) {
			case HttpMessageNotReadableException e -> TipoError.JSON_INVALIDO;
			case NoResourceFoundException e -> TipoError.NO_ENCONTRADO;
			case NoHandlerFoundException e -> TipoError.NO_ENCONTRADO;
			case HttpRequestMethodNotSupportedException e -> TipoError.METODO_NO_PERMITIDO;
			case HttpMediaTypeNotSupportedException e -> TipoError.TIPO_DE_CONTENIDO_NO_SOPORTADO;
			default -> TipoError.INTERNO;
		};
		return respuesta(tipo, headers, request, tipo == TipoError.INTERNO ? ex : null);
	}

	private ResponseEntity<Object> respuesta(TipoError tipo, HttpHeaders headers, WebRequest request,
			@Nullable Exception causa) {
		if (causa != null) {
			log.error("error interno al atender {}", descripcion(request), causa);
		}
		return enviar(tipo, problema(tipo), headers, request);
	}

	private static ProblemDetail problema(TipoError tipo) {
		ProblemDetail problema = ProblemDetail.forStatus(tipo.estado());
		problema.setType(tipo.urn());
		problema.setTitle(tipo.titulo());
		return problema;
	}

	private static ResponseEntity<Object> enviar(TipoError tipo, ProblemDetail problema, HttpHeaders headers,
			WebRequest request) {
		Optional<String> sessionId = request instanceof ServletWebRequest servlet
				? SessionIdDelRequest.leer(servlet.getRequest())
				: Optional.empty();
		log.warn("error type={} status={}{}", tipo.urn(), tipo.estado().value(),
				sessionId.map(id -> " session_id=" + id).orElse(""));
		return ResponseEntity.status(tipo.estado()).headers(headers).contentType(PROBLEM_JSON_UTF8).body(problema);
	}

	private static String descripcion(WebRequest request) {
		return request instanceof ServletWebRequest servlet
				? servlet.getRequest().getMethod() + " " + servlet.getRequest().getRequestURI()
				: "la petición";
	}
}
