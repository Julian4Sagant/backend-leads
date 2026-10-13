package com.sagant.leads.leads_api.evaluacion.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.kie.api.KieBase;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import com.sagant.leads.leads_api.evaluacion.config.CompiladorReglas;
import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;

/**
 * Configuración y reglas reales (src/main/resources) para tests unitarios, sin levantar Spring.
 * La KieBase se compila una sola vez por ejecución de tests.
 */
public final class ReglasDePrueba {

	private static ReglasProperties propiedades;
	private static String drl;
	private static KieBase kieBase;

	private ReglasDePrueba() {
	}

	public static synchronized ReglasProperties propiedades() {
		if (propiedades == null) {
			try {
				List<PropertySource<?>> fuentes = new YamlPropertySourceLoader().load("application",
						new ClassPathResource("application.yml"));
				propiedades = new Binder(ConfigurationPropertySources.from(fuentes))
						.bind("leads.reglas", ReglasProperties.class).get();
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return propiedades;
	}

	public static synchronized String drl() {
		if (drl == null) {
			try (InputStream entrada = new ClassPathResource("reglas/evaluacion.drl").getInputStream()) {
				drl = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return drl;
	}

	public static synchronized KieBase kieBase() {
		if (kieBase == null) {
			kieBase = CompiladorReglas.compilar(drl());
		}
		return kieBase;
	}

	public static MotorReglas motor() {
		return new MotorReglas(kieBase(), propiedades());
	}
}
