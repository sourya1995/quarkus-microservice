package org.acme.transactions;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;


import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {
	@Inject
	@RestClient
	AccountService accountService;

	@ConfigProperty(name = "account.service", defaultValue = "http://localhost:8080")
	String accountServiceUrl;

	@POST
	@Path("/{accountNumber}")
	public Map<String, List<String>> newTransaction(@PathParam("accountNumber") Long accountNumber, BigDecimal amount) {
		try {
			return accountService.transact(accountNumber, amount);
		} catch (Throwable t) {
			t.printStackTrace();
			Map<String, List<String>> response = new HashMap<>();
			response.put("EXCEPTION - " + t.getClass(), Collections.singletonList(t.getMessage()));
			return response;
		}
	}

	@POST
	@Path("/api/{accountNumber}")
	@Bulkhead(1)
	@Fallback(fallbackMethod = "bulkheadFallbackGetBalance", applyOn = { BulkheadException.class })
	public Response newTransactionWithApi(@PathParam("accountNumber") Long accountNumber, BigDecimal amount)
			throws MalformedURLException {
		AccountServiceProgrammatic accountService = (AccountServiceProgrammatic) RestClientBuilder.newBuilder()
				.baseUrl(new URL(accountServiceUrl)).connectTimeout(500, TimeUnit.MILLISECONDS)
				.readTimeout(1400, TimeUnit.MILLISECONDS).build(AccountServiceProgrammatic.class);

		accountService.transact(accountNumber, amount);
		return Response.ok().build();
	}
	
	public Response bulkheadFallbackGetBalance(Long accountNumber, BigDecimal amount) {
		return Response.status(Response.Status.TOO_MANY_REQUESTS).build();
	}
	
	@POST
	@Path("/async/{accountNumber}")
	public CompletionStage<Map<String, List<String>>> newTransactionAsync(@PathParam("accountNumber") Long accountNumber, BigDecimal amount) {
		return accountService.transactAsync(accountNumber, amount);
	}
	
	@POST
	@Path("/api/async/{accountNumber}")
	public CompletionStage<Void> newTransactionWithApiAsync(@PathParam("accountNumber") Long accountNumber, BigDecimal amount)
			throws MalformedURLException {
		AccountServiceProgrammatic accountService = (AccountServiceProgrammatic) RestClientBuilder.newBuilder()
				.baseUrl(new URL(accountServiceUrl)).connectTimeout(500, TimeUnit.MILLISECONDS)
				.readTimeout(1400, TimeUnit.MILLISECONDS).build(AccountServiceProgrammatic.class);

		return accountService.transactAsync(accountNumber, amount);
		
	}
	
	@GET
	@Path("/{accountNumber}/balance")
	@Timeout(100)
//	@Retry(delay=100, jitter=25, maxRetries=3, retryOn = TimeoutException.class)
	@CircuitBreaker(requestVolumeThreshold = 3, failureRatio = .66, delay = 5, delayUnit = ChronoUnit.SECONDS, successThreshold = 2)
//	@Fallback(fallbackMethod = "timeoutFallbackGetBalance")
	@Fallback(value = TransactionServiceFallbackHandler.class)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBalance(@PathParam("accountNumber") Long accountNumber) {
		String balance = accountService.getBalance(accountNumber).toString();
		return Response.ok(balance).build();
	}
	
	public Response timeoutFallbackGetBalance(Long accountNumber) {
		return Response.status(Response.Status.GATEWAY_TIMEOUT).build();
	}
}
