package com.sagant.leads.leads_api.seguridad.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

import com.sagant.leads.leads_api.comun.exception.CuerpoDemasiadoGrandeException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita el cuerpo de toda petición a {@link #MAXIMO_BYTES}. Corre justo después de
 * {@link FiltroClaveApi}, así que sin clave la respuesta sigue siendo 401.
 *
 * <p>Dos controles: si el Content-Length declara más que el límite, responde 413 sin leer el
 * cuerpo, antes de mirar la ruta, el método o el Content-Type. Si no lo declara (chunked), cuenta
 * los bytes a medida que se leen y corta al pasar el límite; una ruta que no lee el cuerpo responde
 * como siempre. El 413 lo arma el manejador único de errores.
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
		chain.doFilter(new RequestLimitado(request), response);
	}

	/**
	 * Termina de leer el cuerpo, con el mismo límite, si alguien ya empezó a leerlo. Jackson deja de
	 * leer cuando el JSON falla o el objeto termina; sin esto, un cuerpo chunked de más de
	 * {@link #MAXIMO_BYTES} con un JSON roto al principio, o con basura después del objeto, no
	 * llegaría al límite. Un cuerpo que nadie abrió no se lee: esa ruta responde como siempre.
	 *
	 * @throws CuerpoDemasiadoGrandeException si el cuerpo completo pasa el límite
	 */
	public static void leerElResto(HttpServletRequest request) throws IOException {
		RequestLimitado limitado = WebUtils.getNativeRequest(request, RequestLimitado.class);
		if (limitado != null && limitado.stream != null) {
			limitado.stream.transferTo(OutputStream.nullOutputStream());
		}
	}

	/** Request cuyo cuerpo, leído como stream o como reader, corta al pasar el límite. */
	static final class RequestLimitado extends HttpServletRequestWrapper {

		private ServletInputStream stream;
		private BufferedReader reader;

		RequestLimitado(HttpServletRequest request) {
			super(request);
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (stream == null) {
				stream = new StreamLimitado(super.getInputStream());
			}
			return stream;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			if (reader == null) {
				String encoding = getCharacterEncoding();
				Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
				reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
			}
			return reader;
		}
	}

	/** Cuenta los bytes leídos y lanza {@link CuerpoDemasiadoGrandeException} al pasar el límite. */
	static final class StreamLimitado extends ServletInputStream {

		private final ServletInputStream original;
		private long leidos;

		StreamLimitado(ServletInputStream original) {
			this.original = original;
		}

		@Override
		public int read() throws IOException {
			int b = original.read();
			if (b != -1) {
				contar(1);
			}
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int n = original.read(b, off, len);
			if (n > 0) {
				contar(n);
			}
			return n;
		}

		private void contar(int n) {
			leidos += n;
			if (leidos > MAXIMO_BYTES) {
				throw new CuerpoDemasiadoGrandeException();
			}
		}

		@Override
		public boolean isFinished() {
			return original.isFinished();
		}

		@Override
		public boolean isReady() {
			return original.isReady();
		}

		@Override
		public void setReadListener(ReadListener listener) {
			original.setReadListener(listener);
		}
	}
}
