package com.sagant.leads.leads_api.evaluacion.config;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;

/**
 * Compila el DRL en memoria, sin kmodule.xml ni escaneo de classpath (así funciona dentro del jar
 * ejecutable). Si el DRL tiene errores, lanza una excepción: la aplicación no arranca.
 */
public final class CompiladorReglas {

	private static final String RUTA_VIRTUAL = "src/main/resources/reglas/evaluacion.drl";

	private CompiladorReglas() {
	}

	public static KieBase compilar(String drl) {
		KieServices servicios = KieServices.Factory.get();
		KieFileSystem archivos = servicios.newKieFileSystem();
		archivos.write(RUTA_VIRTUAL, drl);
		KieBuilder builder = servicios.newKieBuilder(archivos).buildAll();
		if (builder.getResults().hasMessages(Message.Level.ERROR)) {
			throw new IllegalStateException("El archivo de reglas tiene errores: " + builder.getResults().getMessages());
		}
		return servicios.newKieContainer(builder.getKieModule().getReleaseId()).getKieBase();
	}
}
