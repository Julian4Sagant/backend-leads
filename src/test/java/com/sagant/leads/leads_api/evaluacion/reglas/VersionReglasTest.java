package com.sagant.leads.leads_api.evaluacion.reglas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties;
import com.sagant.leads.leads_api.evaluacion.config.ReglasProperties.Puntajes;

class VersionReglasTest {

	private final ReglasProperties reglas = ReglasDePrueba.propiedades();
	private final String drl = ReglasDePrueba.drl();
	private final String version = VersionReglas.calcular(reglas, drl).valor();

	@Test
	void doceCaracteresHexadecimalesEnMinuscula() {
		assertThat(version).matches("^[0-9a-f]{12}$");
	}

	@Test
	void mismaEntradaMismaVersion() {
		assertThat(VersionReglas.calcular(ReglasDePrueba.propiedades(), ReglasDePrueba.drl()).valor())
				.isEqualTo(version);
	}

	@Test
	void unPuntajeDistintoCambiaLaVersion() {
		assertThat(VersionReglas.calcular(conDocumentacionSi(new BigDecimal("16")), drl).valor()).isNotEqualTo(version);
	}

	@Test
	void veinteYVeintePuntoCeroDanLaMismaVersion() {
		ReglasProperties conVeinte = conUrgenciaEstaSemana(new BigDecimal("20"));
		ReglasProperties conVeintePuntoCero = conUrgenciaEstaSemana(new BigDecimal("20.0"));
		assertThat(VersionReglas.calcular(conVeintePuntoCero, drl)).isEqualTo(VersionReglas.calcular(conVeinte, drl));
		assertThat(VersionReglas.calcular(conVeinte, drl).valor()).isEqualTo(version);
	}

	@Test
	void unDrlDistintoCambiaLaVersion() {
		String otro = drl + "\nrule \"regla-de-prueba\"\n    @regla(\"urgencia\")\nwhen\nthen\nend\n";
		assertThat(VersionReglas.calcular(reglas, otro).valor()).isNotEqualTo(version);
	}

	@Test
	void losFinalesDeLineaNoCambianLaVersion() {
		String lf = drl.replace("\r\n", "\n");
		String crlf = lf.replace("\n", "\r\n");
		assertThat(VersionReglas.calcular(reglas, crlf)).isEqualTo(VersionReglas.calcular(reglas, lf));
	}

	@Test
	void otraRevisionDeLaLogicaCambiaLaVersion() {
		assertThat(VersionReglas.calcular(reglas, drl, VersionReglas.REVISION_LOGICA + 1).valor()).isNotEqualTo(version);
	}

	@Test
	void laRutaDelArchivoNoCambiaLaVersion() {
		var otraRuta = new ReglasProperties("classpath:otra/ruta.drl", reglas.prioridad(), reglas.umbrales(),
				reglas.puntajes());
		assertThat(VersionReglas.calcular(otraRuta, drl).valor()).isEqualTo(version);
	}

	private ReglasProperties conUrgenciaEstaSemana(BigDecimal valor) {
		Puntajes p = reglas.puntajes();
		var u = p.urgencia();
		var urgencia = new Puntajes.Urgencia(valor, u.esteMes(), u.de1A3Meses(), u.sinApuro());
		return conPuntajes(new Puntajes(urgencia, p.documentacion(), p.destino(), p.situacionLaboral(),
				p.antiguedad(), p.clearing(), p.carga()));
	}

	private ReglasProperties conDocumentacionSi(BigDecimal valor) {
		Puntajes p = reglas.puntajes();
		var documentacion = new Puntajes.Documentacion(valor, p.documentacion().no());
		return conPuntajes(new Puntajes(p.urgencia(), documentacion, p.destino(), p.situacionLaboral(),
				p.antiguedad(), p.clearing(), p.carga()));
	}

	private ReglasProperties conPuntajes(Puntajes puntajes) {
		return new ReglasProperties(reglas.archivo(), reglas.prioridad(), reglas.umbrales(), puntajes);
	}
}
