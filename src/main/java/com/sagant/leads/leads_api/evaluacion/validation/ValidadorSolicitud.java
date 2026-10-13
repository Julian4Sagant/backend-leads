package com.sagant.leads.leads_api.evaluacion.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sagant.leads.leads_api.evaluacion.dto.ErrorCampo;
import com.sagant.leads.leads_api.evaluacion.dto.ResultadoValidacion;
import com.sagant.leads.leads_api.evaluacion.dto.SolicitudCruda;
import com.sagant.leads.leads_api.evaluacion.dto.ValorCrudo;
import com.sagant.leads.leads_api.evaluacion.enums.Clearing;
import com.sagant.leads.leads_api.evaluacion.enums.CodigoContrato;
import com.sagant.leads.leads_api.evaluacion.enums.Destino;
import com.sagant.leads.leads_api.evaluacion.enums.Documentacion;
import com.sagant.leads.leads_api.evaluacion.enums.SituacionLaboral;
import com.sagant.leads.leads_api.evaluacion.enums.Urgencia;
import com.sagant.leads.leads_api.evaluacion.model.Solicitud;

/**
 * Valida los 12 campos de una {@link SolicitudCruda} y acumula todos los errores en orden de
 * campo. Para cada campo se informa un solo error, el primero que aplica: requerido, tipo,
 * formato, valor permitido, rango.
 *
 * <p>Números: se aceptan como número JSON o string con el mismo valor. En este change el formato
 * es el de {@link BigDecimal}; las regex estrictas llegan con
 * {@code leads-api-endurecer-formato-numerico}. Nunca se redondea ni se trunca.
 */
@Component
public class ValidadorSolicitud {

	private static final Pattern SESSION_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
	private static final int MAX_DECIMALES_IMPORTE = 2;
	private static final int MAX_DIGITOS_ENTEROS_IMPORTE = 12;

	public ResultadoValidacion validar(SolicitudCruda cruda) {
		if (!cruda.esObjeto()) {
			return new ResultadoValidacion(Optional.empty(),
					List.of(new ErrorCampo(ErrorCampo.RAIZ, ErrorCampo.TIPO_INVALIDO, "se esperaba un objeto JSON")),
					Optional.empty());
		}
		Lectura l = new Lectura(cruda);
		String sessionId = l.sessionId();
		Integer edad = l.entero("edad", 1, 100);
		Destino destino = l.codigo("motivo", Destino.class);
		BigDecimal monto = l.importe("monto", Minimo.MAYOR_QUE_CERO);
		Integer plazoMeses = l.entero("plazo_meses", 1, 120);
		Urgencia urgencia = l.codigo("urgencia", Urgencia.class);
		SituacionLaboral situacion = l.codigo("situacion_laboral", SituacionLaboral.class);
		Integer antiguedadMeses = l.entero("antiguedad_meses", 0, 600);
		BigDecimal ingresoMensual = l.importe("ingreso_mensual", Minimo.CERO);
		BigDecimal cuotaDeudas = l.importe("cuota_deudas", Minimo.CERO);
		Clearing clearing = l.codigo("clearing", Clearing.class);
		Documentacion documentacion = l.codigo("documentacion", Documentacion.class);

		Optional<String> sessionIdValido = Optional.ofNullable(sessionId);
		if (!l.errores.isEmpty()) {
			return new ResultadoValidacion(Optional.empty(), l.errores, sessionIdValido);
		}
		Solicitud solicitud = new Solicitud(sessionId, edad, destino, monto, plazoMeses, urgencia, situacion,
				antiguedadMeses, ingresoMensual, cuotaDeudas, clearing, documentacion);
		return new ResultadoValidacion(Optional.of(solicitud), List.of(), sessionIdValido);
	}

	private enum Minimo {
		CERO, MAYOR_QUE_CERO
	}

	/** Lee los campos de una solicitud cruda y acumula los errores. Cada método devuelve null si hubo error. */
	private static final class Lectura {

		private final SolicitudCruda cruda;
		private final List<ErrorCampo> errores = new ArrayList<>();

		Lectura(SolicitudCruda cruda) {
			this.cruda = cruda;
		}

		String sessionId() {
			String texto = texto("session_id");
			if (texto == null) {
				return null;
			}
			if (!SESSION_ID.matcher(texto).matches()) {
				return error("session_id", ErrorCampo.FORMATO_INVALIDO,
						"debe tener de 1 a 64 caracteres ASCII: letras, dígitos, guion o guion bajo");
			}
			return texto;
		}

		<E extends Enum<E> & CodigoContrato> E codigo(String campo, Class<E> tipo) {
			String texto = texto(campo);
			if (texto == null) {
				return null;
			}
			Optional<E> valor = CodigoContrato.desdeCodigo(tipo, texto);
			if (valor.isEmpty()) {
				String posibles = Arrays.stream(tipo.getEnumConstants()).map(CodigoContrato::codigo)
						.collect(Collectors.joining(", "));
				return error(campo, ErrorCampo.VALOR_NO_PERMITIDO, "valor no permitido; valores posibles: " + posibles);
			}
			return valor.get();
		}

		Integer entero(String campo, int minimo, int maximo) {
			BigDecimal numero = numero(campo);
			if (numero == null) {
				return null;
			}
			if (numero.scale() > 0) {
				return error(campo, ErrorCampo.FORMATO_INVALIDO, "debe ser un número entero, sin decimales");
			}
			if (numero.compareTo(BigDecimal.valueOf(minimo)) < 0 || numero.compareTo(BigDecimal.valueOf(maximo)) > 0) {
				return error(campo, ErrorCampo.FUERA_DE_RANGO, "debe estar entre " + minimo + " y " + maximo);
			}
			return numero.intValueExact();
		}

		BigDecimal importe(String campo, Minimo minimo) {
			BigDecimal numero = numero(campo);
			if (numero == null) {
				return null;
			}
			if (numero.scale() > MAX_DECIMALES_IMPORTE) {
				return error(campo, ErrorCampo.FORMATO_INVALIDO, "debe tener como máximo 2 decimales");
			}
			if (numero.precision() - numero.scale() > MAX_DIGITOS_ENTEROS_IMPORTE) {
				return error(campo, ErrorCampo.FORMATO_INVALIDO, "debe tener como máximo 12 dígitos enteros");
			}
			int comparacion = numero.signum();
			if (minimo == Minimo.MAYOR_QUE_CERO && comparacion <= 0) {
				return error(campo, ErrorCampo.FUERA_DE_RANGO, "debe ser mayor que 0");
			}
			if (minimo == Minimo.CERO && comparacion < 0) {
				return error(campo, ErrorCampo.FUERA_DE_RANGO, "debe ser mayor o igual a 0");
			}
			return numero;
		}

		/** Número JSON o string numérico; null si hubo error. */
		private BigDecimal numero(String campo) {
			ValorCrudo valor = presente(campo);
			if (valor == null) {
				return null;
			}
			if (valor.tipo() != ValorCrudo.Tipo.NUMERO && valor.tipo() != ValorCrudo.Tipo.STRING) {
				return error(campo, ErrorCampo.TIPO_INVALIDO, "se esperaba un número");
			}
			try {
				return new BigDecimal(valor.texto());
			}
			catch (NumberFormatException e) {
				return error(campo, ErrorCampo.FORMATO_INVALIDO, "no es un número válido");
			}
		}

		/** String JSON; null si hubo error. */
		private String texto(String campo) {
			ValorCrudo valor = presente(campo);
			if (valor == null) {
				return null;
			}
			if (valor.tipo() != ValorCrudo.Tipo.STRING) {
				return error(campo, ErrorCampo.TIPO_INVALIDO, "se esperaba un texto");
			}
			return valor.texto();
		}

		/** El valor si está y no es null; si no, registra {@code requerido}. */
		private ValorCrudo presente(String campo) {
			Optional<ValorCrudo> valor = cruda.campo(campo);
			if (valor.isEmpty() || valor.get().tipo() == ValorCrudo.Tipo.NULO) {
				return error(campo, ErrorCampo.REQUERIDO, "es requerido");
			}
			return valor.get();
		}

		private <T> T error(String campo, String codigo, String mensaje) {
			errores.add(new ErrorCampo(campo, codigo, mensaje));
			return null;
		}
	}
}
