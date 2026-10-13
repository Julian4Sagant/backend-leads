package com.sagant.leads.leads_api.comun.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sagant.leads.leads_api.comun.enums.TipoError;
import com.sagant.leads.leads_api.evaluacion.controller.PruebaHttp;
import com.sagant.leads.leads_api.evaluacion.service.Evaluador;

/** Tarea 6.15: error interno y catálogo de type, con el evaluador simulado para que falle. */
class ErrorInternoHttpTest extends PruebaHttp {

	@MockitoBean
	private Evaluador evaluador;

	@Test
	@DisplayName("Error interno sin detalles")
	void errorInternoSinDetalles() throws Exception {
		when(evaluador.evaluar(any())).thenThrow(new IllegalStateException("detalle-interno-de-prueba"));
		Respuesta r = evaluar(base());
		assertError(r, 500, "interno");
		assertContentType(r, "application/problem+json;charset=UTF-8");
		assertThat(r.cuerpo()).doesNotContain("detalle-interno-de-prueba").doesNotContain("IllegalStateException")
				.doesNotContain("at com.").doesNotContain("Exception");
		assertThat(r.json().has("detail")).isFalse();
	}

	@Test
	@DisplayName("Cada type usado está en el catálogo")
	void cadaTypeUsadoEstaEnElCatalogo() throws Exception {
		when(evaluador.evaluar(any())).thenThrow(new IllegalStateException("detalle-interno-de-prueba"));
		List<Respuesta> respuestas = List.of(
				evaluar(con("edad", 101)),
				evaluarCrudo("{\"edad\":"),
				enviar(post("/evaluar").contentType(MediaType.APPLICATION_JSON).content("{}")),
				enviar(get("/no-existe").header("X-API-Key", clave)),
				enviar(get("/evaluar").header("X-API-Key", clave)),
				enviar(post("/evaluar").header("X-API-Key", clave).contentType(MediaType.TEXT_PLAIN).content("{}")),
				evaluar(base()));
		assertThat(respuestas).extracting(Respuesta::status).containsExactly(400, 400, 401, 404, 405, 415, 500);
		for (Respuesta r : respuestas) {
			TipoError tipo = java.util.Arrays.stream(TipoError.values())
					.filter(t -> t.urn().toString().equals(r.type())).findFirst().orElseThrow();
			assertThat(tipo.estado().value()).isEqualTo(r.status());
			assertThat(r.json().path("status").asInt()).isEqualTo(r.status());
		}
	}
}
