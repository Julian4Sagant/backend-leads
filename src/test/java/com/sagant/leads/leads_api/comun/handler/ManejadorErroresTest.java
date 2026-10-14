package com.sagant.leads.leads_api.comun.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import com.sagant.leads.leads_api.comun.enums.TipoError;
import com.sagant.leads.leads_api.comun.exception.CuerpoDemasiadoGrandeException;

/** Respuesta del manejador al cuerpo demasiado grande, sin Spring MVC. */
class ManejadorErroresTest {

	private final ManejadorErrores manejador = new ManejadorErrores();

	@Test
	void cuerpoDemasiadoGrandeDa413ConElTypeDelCatalogoYSinErrores() {
		ResponseEntity<Object> r = manejador.cuerpoDemasiadoGrande(new CuerpoDemasiadoGrandeException(),
				new ServletWebRequest(new MockHttpServletRequest()));
		assertThat(r.getStatusCode().value()).isEqualTo(413);
		assertThat(r.getHeaders().getContentType()).isEqualTo(ManejadorErrores.PROBLEM_JSON_UTF8);
		ProblemDetail problema = (ProblemDetail) r.getBody();
		assertThat(problema.getType()).isEqualTo(TipoError.CUERPO_DEMASIADO_GRANDE.urn());
		assertThat(problema.getStatus()).isEqualTo(413);
		// Sin propiedades extra, ProblemDetail devuelve null en vez de un mapa vacío.
		assertThat(Objects.requireNonNullElse(problema.getProperties(), Map.of())).doesNotContainKey("errores");
	}
}
