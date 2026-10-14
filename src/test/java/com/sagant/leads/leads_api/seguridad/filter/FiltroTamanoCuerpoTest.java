package com.sagant.leads.leads_api.seguridad.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.sagant.leads.leads_api.comun.exception.CuerpoDemasiadoGrandeException;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Los dos controles del filtro, sin Spring: borde exacto del Content-Length y, sin Content-Length,
 * borde exacto de los bytes leídos (en bloque, de a uno y con el reader).
 */
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

	@ParameterizedTest(name = "sin Content-Length: {0} bytes se leen completos")
	@ValueSource(ints = { 0, 16383, 16384 })
	void sinContentLengthHastaElLimiteSeLeeCompleto(int bytes) throws Exception {
		HttpServletRequest leido = filtrarSinContentLength(bytes);
		assertThat(leido.getInputStream().readAllBytes()).hasSize(bytes);
	}

	@Test
	void sinContentLengthElByte16385Corta() throws Exception {
		HttpServletRequest leido = filtrarSinContentLength(16385);
		assertThatThrownBy(() -> leido.getInputStream().readAllBytes())
				.isInstanceOf(CuerpoDemasiadoGrandeException.class);
	}

	@Test
	void sinContentLengthElByte16385CortaLeyendoDeAUno() throws Exception {
		ServletInputStream stream = filtrarSinContentLength(16385).getInputStream();
		for (int i = 0; i < 16384; i++) {
			assertThat(stream.read()).isNotEqualTo(-1);
		}
		assertThatThrownBy(stream::read).isInstanceOf(CuerpoDemasiadoGrandeException.class);
	}

	@Test
	void sinContentLengthElReaderTambienCorta() throws Exception {
		HttpServletRequest leido = filtrarSinContentLength(16385);
		assertThatThrownBy(() -> leido.getReader().transferTo(Writer.nullWriter()))
				.isInstanceOf(CuerpoDemasiadoGrandeException.class);
	}

	@Test
	void leerElRestoDeUnCuerpoEmpezadoQuePasaElLimiteDa413() throws Exception {
		HttpServletRequest leido = filtrarSinContentLength(20480);
		leido.getInputStream().readNBytes(100);
		assertThatThrownBy(() -> FiltroTamanoCuerpo.leerElResto(leido))
				.isInstanceOf(CuerpoDemasiadoGrandeException.class);
	}

	@Test
	void leerElRestoDeUnCuerpoEmpezadoDentroDelLimiteNoFalla() throws Exception {
		HttpServletRequest leido = filtrarSinContentLength(16384);
		leido.getInputStream().readNBytes(100);
		FiltroTamanoCuerpo.leerElResto(leido);
		assertThat(leido.getInputStream().read()).isEqualTo(-1);
	}

	@Test
	void leerElRestoNoAbreUnCuerpoQueNadieEmpezo() throws Exception {
		HttpServletRequest leido = filtrarSinContentLength(20480);
		FiltroTamanoCuerpo.leerElResto(leido);
		assertThat(leido.getInputStream().readNBytes(100)).hasSize(100);
	}

	/** Pasa por el filtro un request chunked (sin Content-Length) y devuelve el que recibe la cadena. */
	private HttpServletRequest filtrarSinContentLength(int bytes) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/evaluar") {

			@Override
			public int getContentLength() {
				return -1;
			}

			@Override
			public long getContentLengthLong() {
				return -1;
			}
		};
		request.setContent("x".repeat(bytes).getBytes(StandardCharsets.UTF_8));
		MockFilterChain cadena = new MockFilterChain();
		filtro.doFilter(request, new MockHttpServletResponse(), cadena);
		return (HttpServletRequest) cadena.getRequest();
	}

	private static MockHttpServletRequest conCuerpo(int bytes) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/evaluar");
		request.setContent(new byte[bytes]);
		return request;
	}
}
