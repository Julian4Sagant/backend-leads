package com.sagant.leads.leads_api.evaluacion.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.kie.api.KieBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.sagant.leads.leads_api.evaluacion.service.VersionReglas;

@Configuration(proxyBeanMethods = false)
public class ReglasConfig {

	@Bean
	ArchivoReglas archivoReglas(ReglasProperties reglas, ResourceLoader resourceLoader) {
		Resource recurso = resourceLoader.getResource(reglas.archivo());
		try (InputStream entrada = recurso.getInputStream()) {
			return new ArchivoReglas(reglas.archivo(), new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer el archivo de reglas " + reglas.archivo(), e);
		}
	}

	@Bean
	KieBase kieBase(ArchivoReglas archivo) {
		return CompiladorReglas.compilar(archivo.contenido());
	}

	/** Se calcula una sola vez, al arrancar. */
	@Bean
	VersionReglas versionReglas(ReglasProperties reglas, ArchivoReglas archivo) {
		return VersionReglas.calcular(reglas, archivo.contenido());
	}
}
