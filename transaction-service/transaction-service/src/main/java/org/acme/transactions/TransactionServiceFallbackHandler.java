package org.acme.transactions;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

public class TransactionServiceFallbackHandler implements FallbackHandler<Response> {

	private static final Logger LOGGER = Logger.getLogger(TransactionServiceFallbackHandler.class);

	@Inject
	@RegistryType(type = MetricRegistry.Type.APPLICATION)
	MetricRegistry metricRegistry;

	@Timed(name = "fallbackHandlerTimer", displayName = "Fallback Handler Timer", description = "Time spent handling fallbacks", absolute = true, unit = MetricUnits.NANOSECONDS)

	@Override
	public Response handle(ExecutionContext context) {
		Response response;
		String name;

		if (context.getFailure().getCause() == null) {
			name = context.getFailure().getClass().getSimpleName();
		} else {
			name = context.getFailure().getCause().getClass().getSimpleName();
		}

		switch (name) {
		case "BulkheadException":
			response = Response.status(Response.Status.TOO_MANY_REQUESTS).build();
			break;
		case "TimeoutException":
			response = Response.status(Response.Status.GATEWAY_TIMEOUT).build();
			break;
		case "CircuitBreakerOpenException":
		case "ConnectTimeoutException":
		case "SocketException":
			response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
			break;
		case "ResteasyWebApplicationException":
		case "WebApplicationException":
		case "HttpHostConnectException":
			response = Response.status(Response.Status.BAD_GATEWAY).build();
			break;
		default:
			response = Response.status(Response.Status.NOT_IMPLEMENTED).build();
		}

		metricRegistry.counter("fallback", new Tag("http_status_code", "" + response.getStatus())).inc();

		LOGGER.info("**********" + context.getMethod().getName() + ": " + name + "***********");
		return response;
	}

}
