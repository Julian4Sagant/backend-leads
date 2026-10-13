package com.sagant.leads.leads_api.seguridad;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.sagant.leads.leads_api.comun.error.NoAutorizadoException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exige X-API-Key en toda petición, a cualquier ruta y con cualquier método. Corre antes que
 * cualquier otro filtro y nunca lee el cuerpo. El 401 lo arma el manejador único de errores:
 * el filtro le delega la excepción.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroClaveApi extends OncePerRequestFilter {

	static final String HEADER = "X-API-Key";

	private final ClaveApi clave;
	private final HandlerExceptionResolver resolver;

	public FiltroClaveApi(ClaveApi clave, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.clave = clave;
		this.resolver = resolver;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (clave.coincide(request.getHeader(HEADER))) {
			chain.doFilter(request, response);
		}
		else {
			resolver.resolveException(request, response, null, new NoAutorizadoException());
		}
	}
}
