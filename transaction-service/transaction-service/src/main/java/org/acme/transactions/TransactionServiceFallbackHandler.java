package org.acme.transactions;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Response;

public class TransactionServiceFallbackHandler implements FallbackHandler<Response>{
	
	private static final Logger LOGGER = Logger.getLogger(TransactionServiceFallbackHandler.class);

	@Override
	public Response handle(ExecutionContext context) {
		Response response;
		String name;
		
		if(context.getFailure().getCause() == null) {
			name = context.getFailure().getClass().getSimpleName();
		} else {
			name = context.getFailure().getCause().getClass().getSimpleName();
		}
		
		switch(name) {
			case "BulkheadException":
				response = Response.status(Response.Status.TOO_MANY_REQUESTS).build();
				break;
			case "TimeoutException":
				response = Response.status(Response.Status.GATEWAY_TIMEOUT).build();
				break;
			case "CircuitBreakerOpenException":
				response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
				break;
			case "WebApplicationException":
			case "HttpHostConnectException":
				response = Response.status(Response.Status.BAD_GATEWAY).build();
				break;
			default:
				response = Response.status(Response.Status.NOT_IMPLEMENTED).build();
		}
		
		LOGGER.info("**********" + context.getMethod().getName() + ": " + name + "***********");
		return response;
	}

}
