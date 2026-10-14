package com.sagant.leads.leads_api.seguridad.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.sagant.leads.leads_api.comun.exception.CuerpoDemasiadoGrandeException;

/** Control del tamaño declarado, sin Spring: borde exacto del Content-Length. */
class FiltroTamanoCuerpoTest {

	private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
	private final FiltroTamanoCuerpo filtro = new FiltroTamanoCuerpo(resolver);

	@ParameterizedTest(name = "Content-Length {0} sigue la cadena")
	@ValueSource(ints = { 0, 300, 16384 })
	void hastaElLimiteSigueLaCadena(int bytes) throws Exception {
		MockFilterChain cadena = new MockFilterChain();
		filtro.doFilter(conCuerpo(bytes), new MockHttpServletResponse(), cadena);
		assertThat(cadena.getRequest()).isNotNull();
		verify(resolver, never()).resolveException(any(), any(), any(), any());
	}

	@Test
	void unByteMasNoSigueLaCadenaYDelegaEl413() throws Exception {
		MockFilterChain cadena = new MockFilterChain();
		filtro.doFilter(conCuerpo(16385), new MockHttpServletResponse(), cadena);
		assertThat(cadena.getRequest()).isNull();
		verify(resolver).resolveException(any(), any(), isNull(), any(CuerpoDemasiadoGrandeException.class));
	}

	private static MockHttpServletRequest conCuerpo(int bytes) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/evaluar");
		request.setContent(new byte[bytes]);
		return request;
	}
}
