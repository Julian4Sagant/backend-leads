package com.sagant.leads.leads_api.seguridad.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.sagant.leads.leads_api.seguridad.filter.FiltroTamanoCuerpo;

/**
 * Después de leer un {@code @RequestBody}, termina de leer el cuerpo con el límite de
 * {@link FiltroTamanoCuerpo}: Jackson para al cerrar el objeto, y un cuerpo chunked con basura
 * después del JSON no llegaría al límite. Si en total lo pasa, el 413 sale antes de evaluar.
 */
@ControllerAdvice
public class LimiteCuerpoLeido extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter parameter, Type tipo, Class<? extends HttpMessageConverter<?>> conversor) {
		return true;
	}

	@Override
	public Object afterBodyRead(Object cuerpo, HttpInputMessage mensaje, MethodParameter parameter, Type tipo,
			Class<? extends HttpMessageConverter<?>> conversor) {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
			try {
				FiltroTamanoCuerpo.leerElResto(atributos.getRequest());
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		return cuerpo;
	}
}
