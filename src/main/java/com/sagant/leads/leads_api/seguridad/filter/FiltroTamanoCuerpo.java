package com.sagant.leads.leads_api.seguridad.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.sagant.leads.leads_api.comun.exception.CuerpoDemasiadoGrandeException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita el cuerpo de toda petición a {@link #MAXIMO_BYTES}. Corre justo después de
 * {@link FiltroClaveApi}, así que sin clave la respuesta sigue siendo 401. Si el Content-Length
 * declara más que el límite, responde 413 sin leer el cuerpo, antes de mirar la ruta, el método o
 * el Content-Type. El 413 lo arma el manejador único de errores: el filtro le delega la excepción.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class FiltroTamanoCuerpo extends OncePerRequestFilter {

	/** 16 KB. Es parte del contrato: cambiarlo pasa por un change. */
	public static final int MAXIMO_BYTES = 16384;

	private final HandlerExceptionResolver resolver;

	public FiltroTamanoCuerpo(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (request.getContentLengthLong() > MAXIMO_BYTES) {
			resolver.resolveException(request, response, null, new CuerpoDemasiadoGrandeException());
			return;
		}
		chain.doFilter(request, response);
	}
}
