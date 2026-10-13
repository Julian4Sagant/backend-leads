package com.sagant.leads.leads_api.evaluacion.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sagant.leads.leads_api.evaluacion.dto.ValorCrudo.Tipo;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class LectorSolicitudCrudaTest {

	private final JsonMapper mapper = JsonMapper.builder().build();

	/** Cada valor conserva su tipo de token y, si es número o string, su texto original. */
	@ParameterizedTest(name = "monto: {0} -> {1} {2}")
	@CsvSource(delimiter = '|', nullValues = "NULL", value = {
			"150000|NUMERO|150000",
			"\"150000\"|STRING|150000",
			"150000.00|NUMERO|150000.00",
			"1e3|NUMERO|1e3",
			"-0.01|NUMERO|-0.01",
			"\"\"|STRING|",
			"true|BOOLEANO|NULL",
			"false|BOOLEANO|NULL",
			"null|NULO|NULL",
			"[]|ARRAY|NULL",
			"[1, {\"a\": 2}]|ARRAY|NULL",
			"{}|OBJETO|NULL",
	})
	void conservaTipoYTextoOriginal(String json, Tipo tipo, String texto) {
		SolicitudCruda cruda = leer("{\"monto\": " + json + "}");
		assertThat(cruda.esObjeto()).isTrue();
		assertThat(cruda.campo("monto")).contains(new ValorCrudo(tipo, texto == null && tipo == Tipo.STRING ? "" : texto));
	}

	@Test
	void campoAusente() {
		SolicitudCruda cruda = leer("{\"edad\": 32}");
		assertThat(cruda.campo("monto")).isEmpty();
		assertThat(cruda.campo("edad")).contains(ValorCrudo.numero("32"));
	}

	@Test
	void camposQueNoSonDelContratoSeDescartan() {
		SolicitudCruda cruda = leer("{\"nombre\": \"Persona Ficticia\", \"extra\": {\"a\": [1]}, \"edad\": 32}");
		assertThat(cruda.campos()).containsOnlyKeys("edad");
	}

	@Test
	void campoRepetidoGanaElUltimo() {
		assertThat(leer("{\"edad\": 30, \"edad\": 32}").campo("edad")).contains(ValorCrudo.numero("32"));
	}

	@ParameterizedTest(name = "raíz {0} no es objeto")
	@CsvSource(delimiter = '|', value = { "[1, 2]", "\"hola\"", "5", "true", "null" })
	void raizQueNoEsObjeto(String json) {
		SolicitudCruda cruda = leer(json);
		assertThat(cruda.esObjeto()).isFalse();
		assertThat(cruda.campos()).isEmpty();
	}

	@Test
	void jsonTruncadoNoSePuedeLeer() {
		assertThatThrownBy(() -> leer("{\"edad\": 32,")).isInstanceOf(JacksonException.class);
	}

	@Test
	void numeroDeMasDeMilDigitosNoSePuedeLeer() {
		assertThatThrownBy(() -> leer("{\"monto\": " + "9".repeat(1001) + "}")).isInstanceOf(JacksonException.class);
	}

	private SolicitudCruda leer(String json) {
		return mapper.readValue(json, SolicitudCruda.class);
	}
}
