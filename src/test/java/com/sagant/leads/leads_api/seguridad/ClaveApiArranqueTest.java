package com.sagant.leads.leads_api.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sagant.leads.leads_api.BackendLeadsApplication;

/** Requirement "Clave configurada al arrancar". Todas las claves son ficticias. */
@ExtendWith(OutputCaptureExtension.class)
class ClaveApiArranqueTest {

	private static final String CLAVE_31 = "clave-ficticia-de-31-caracteres";

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(SeguridadProperties.class)
	@Import(ClaveApi.class)
	static class Config {
	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

	@Test
	@DisplayName("Sin clave configurada")
	void sinClaveConfigurada() {
		runner.run(context -> assertFallaSinMostrar(context, "LEADS_API_KEY", null));
		runner.withPropertyValues("leads.seguridad.api-key=").run(context -> assertFallaSinMostrar(context, "LEADS_API_KEY", null));
	}

	@Test
	@DisplayName("Clave de 31 caracteres")
	void claveDe31Caracteres() {
		assertThat(CLAVE_31).hasSize(31);
		runner.withPropertyValues("leads.seguridad.api-key=" + CLAVE_31)
				.run(context -> assertFallaSinMostrar(context, "al menos 32 caracteres", CLAVE_31));
	}

	@Test
	void claveDe32CaracteresArranca() {
		String clave = CLAVE_31 + "x";
		runner.withPropertyValues("leads.seguridad.api-key=" + clave).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(ClaveApi.class).coincide(clave)).isTrue();
			assertThat(context.getBean(ClaveApi.class).coincide(CLAVE_31)).isFalse();
			assertThat(context.getBean(SeguridadProperties.class).toString()).doesNotContain(clave);
		});
	}

	@Test
	@DisplayName("Clave con espacio")
	void claveConEspacio() {
		String clave = "clave-ficticia-con un-espacio-de-40-char";
		assertThat(clave).hasSize(40).contains(" ");
		runner.withPropertyValues("leads.seguridad.api-key=" + clave)
				.run(context -> assertFallaSinMostrar(context, "ASCII imprimibles, sin espacios", clave));
	}

	@Test
	@DisplayName("Clave con carácter no ASCII")
	void claveConCaracterNoAscii() {
		String clave = "clave-ficticia-con-eñe-de-cuarenta-chars";
		assertThat(clave).hasSize(40).contains("ñ");
		runner.withPropertyValues("leads.seguridad.api-key=" + clave)
				.run(context -> assertFallaSinMostrar(context, "ASCII imprimibles, sin espacios", clave));
	}

	@Test
	@DisplayName("Mensaje de arranque sin la clave")
	void mensajeDeArranqueSinLaClave(CapturedOutput salida) {
		assertThatThrownBy(() -> SpringApplication.run(BackendLeadsApplication.class,
				"--leads.seguridad.api-key=" + CLAVE_31, "--server.port=0"))
				.satisfies(e -> assertThat(mensajes(e)).contains("al menos 32 caracteres").doesNotContain(CLAVE_31));
		assertThat(salida.getAll()).contains("al menos 32 caracteres").doesNotContain(CLAVE_31);
	}

	private static void assertFallaSinMostrar(AssertableApplicationContext context, String esperado, String clave) {
		assertThat(context).hasFailed();
		String mensajes = mensajes(context.getStartupFailure());
		assertThat(mensajes).contains(esperado);
		if (clave != null) {
			assertThat(mensajes).doesNotContain(clave);
		}
	}

	private static String mensajes(Throwable error) {
		StringBuilder texto = new StringBuilder();
		for (Throwable t = error; t != null; t = t.getCause()) {
			texto.append(t).append('\n');
		}
		return texto.toString();
	}
}
