package com.sagant.leads.leads_api.evaluacion.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Puntajes y umbrales del motor de reglas ({@code leads.reglas}).
 *
 * <p>Los valores son {@link BigDecimal} para que {@code 20} y {@code 20.0} se acepten igual.
 * Los puntajes, los umbrales de prioridad y los de antigüedad deben ser enteros.
 * La coherencia entre valores se valida al arrancar: si falla, la aplicación no arranca.
 */
@Validated
@ConfigurationProperties("leads.reglas")
public record ReglasProperties(
		@NotBlank String archivo,
		@NotNull @Valid Prioridad prioridad,
		@NotNull @Valid Umbrales umbrales,
		@NotNull @Valid Puntajes puntajes) {

	/** Umbrales de score: ALTA con score >= alta; MEDIA con score >= media. */
	public record Prioridad(
			@NotNull @Entero @DecimalMin("0") BigDecimal alta,
			@NotNull @Entero @DecimalMin("0") BigDecimal media) {

		@AssertTrue(message = "leads.reglas.prioridad.alta debe ser mayor que leads.reglas.prioridad.media")
		public boolean isAltaMayorQueMedia() {
			return alta == null || media == null || alta.compareTo(media) > 0;
		}
	}

	public record Umbrales(
			@NotNull @Valid Antiguedad antiguedad,
			@NotNull @Valid Carga carga) {

		/** Antigüedad en meses: tramo alto desde mesesAlto; tramo medio desde mesesMedio. */
		public record Antiguedad(
				@NotNull @Entero @Positive BigDecimal mesesAlto,
				@NotNull @Entero @Positive BigDecimal mesesMedio) {

			@AssertTrue(message = "leads.reglas.umbrales.antiguedad.meses-medio debe ser menor que leads.reglas.umbrales.antiguedad.meses-alto")
			public boolean isMedioMenorQueAlto() {
				return mesesAlto == null || mesesMedio == null || mesesMedio.compareTo(mesesAlto) < 0;
			}
		}

		/** Fracciones del ingreso: tramo bajo hasta baja; tramo medio hasta alta. */
		public record Carga(
				@NotNull @Positive BigDecimal baja,
				@NotNull @Positive BigDecimal alta) {

			@AssertTrue(message = "leads.reglas.umbrales.carga.baja debe ser menor que leads.reglas.umbrales.carga.alta")
			public boolean isBajaMenorQueAlta() {
				return baja == null || alta == null || baja.compareTo(alta) < 0;
			}
		}
	}

	public record Puntajes(
			@NotNull @Valid Urgencia urgencia,
			@NotNull @Valid Documentacion documentacion,
			@NotNull @Valid Destino destino,
			@NotNull @Valid SituacionLaboral situacionLaboral,
			@NotNull @Valid Antiguedad antiguedad,
			@NotNull @Valid Clearing clearing,
			@NotNull @Valid Carga carga) {

		public record Urgencia(
				@NotNull @Entero BigDecimal estaSemana,
				@NotNull @Entero BigDecimal esteMes,
				@NotNull @Entero BigDecimal de1A3Meses,
				@NotNull @Entero BigDecimal sinApuro) {
		}

		public record Documentacion(
				@NotNull @Entero BigDecimal si,
				@NotNull @Entero BigDecimal no) {
		}

		public record Destino(
				@NotNull @Entero BigDecimal definido,
				@NotNull @Entero BigDecimal noDefinido) {
		}

		public record SituacionLaboral(
				@NotNull @Entero BigDecimal dependiente,
				@NotNull @Entero BigDecimal independiente,
				@NotNull @Entero BigDecimal jubilado,
				@NotNull @Entero BigDecimal estudiante,
				@NotNull @Entero BigDecimal desempleado) {
		}

		public record Antiguedad(
				@NotNull @Entero BigDecimal doceOMas,
				@NotNull @Entero BigDecimal seisAOnce,
				@NotNull @Entero BigDecimal menosDeSeis,
				@NotNull @Entero BigDecimal noAplica) {
		}

		public record Clearing(
				@NotNull @Entero BigDecimal si,
				@NotNull @Entero BigDecimal noSe,
				@NotNull @Entero BigDecimal no) {
		}

		public record Carga(
				@NotNull @Entero BigDecimal baja,
				@NotNull @Entero BigDecimal media,
				@NotNull @Entero BigDecimal alta) {
		}
	}
}
