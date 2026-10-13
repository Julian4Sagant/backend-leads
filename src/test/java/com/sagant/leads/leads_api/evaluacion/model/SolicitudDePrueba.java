package com.sagant.leads.leads_api.evaluacion.model;

import java.math.BigDecimal;

import com.sagant.leads.leads_api.evaluacion.enums.Clearing;
import com.sagant.leads.leads_api.evaluacion.enums.Destino;
import com.sagant.leads.leads_api.evaluacion.enums.Documentacion;
import com.sagant.leads.leads_api.evaluacion.enums.SituacionLaboral;
import com.sagant.leads.leads_api.evaluacion.enums.Urgencia;

/**
 * Constructor de solicitudes para tests. Parte de la solicitud base de la spec
 * {@code evaluacion-leads} (score 85, ALTA); cada test cambia solo lo que necesita.
 * Todos los datos son ficticios.
 */
public final class SolicitudDePrueba {

	private String sessionId = "beb55e7eb0c947ff86f19aa0e0d0b2db";
	private int edad = 32;
	private Destino destino = Destino.AUTO;
	private BigDecimal monto = new BigDecimal("150000");
	private int plazoMeses = 24;
	private Urgencia urgencia = Urgencia.ESTA_SEMANA;
	private SituacionLaboral situacionLaboral = SituacionLaboral.DEPENDIENTE;
	private int antiguedadMeses = 36;
	private BigDecimal ingresoMensual = new BigDecimal("60000");
	private BigDecimal cuotaDeudas = new BigDecimal("5000");
	private Clearing clearing = Clearing.NO;
	private Documentacion documentacion = Documentacion.SI;

	private SolicitudDePrueba() {
	}

	public static SolicitudDePrueba base() {
		return new SolicitudDePrueba();
	}

	public SolicitudDePrueba destino(Destino valor) {
		this.destino = valor;
		return this;
	}

	public SolicitudDePrueba monto(String valor) {
		this.monto = new BigDecimal(valor);
		return this;
	}

	public SolicitudDePrueba plazoMeses(int valor) {
		this.plazoMeses = valor;
		return this;
	}

	public SolicitudDePrueba urgencia(Urgencia valor) {
		this.urgencia = valor;
		return this;
	}

	public SolicitudDePrueba situacionLaboral(SituacionLaboral valor) {
		this.situacionLaboral = valor;
		return this;
	}

	public SolicitudDePrueba antiguedadMeses(int valor) {
		this.antiguedadMeses = valor;
		return this;
	}

	public SolicitudDePrueba ingresoMensual(String valor) {
		this.ingresoMensual = new BigDecimal(valor);
		return this;
	}

	public SolicitudDePrueba cuotaDeudas(String valor) {
		this.cuotaDeudas = new BigDecimal(valor);
		return this;
	}

	public SolicitudDePrueba clearing(Clearing valor) {
		this.clearing = valor;
		return this;
	}

	public SolicitudDePrueba documentacion(Documentacion valor) {
		this.documentacion = valor;
		return this;
	}

	public Solicitud build() {
		return new Solicitud(sessionId, edad, destino, monto, plazoMeses, urgencia, situacionLaboral,
				antiguedadMeses, ingresoMensual, cuotaDeudas, clearing, documentacion);
	}
}
