package org.acme.transactions;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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
	public Response newTransactionWithApi(@PathParam("accountNumber") Long accountNumber, BigDecimal amount)
			throws MalformedURLException {
		AccountServiceProgrammatic accountService = (AccountServiceProgrammatic) RestClientBuilder.newBuilder()
				.baseUrl(new URL(accountServiceUrl)).connectTimeout(500, TimeUnit.MILLISECONDS)
				.readTimeout(1400, TimeUnit.MILLISECONDS).build(AccountServiceProgrammatic.class);

		accountService.transact(accountNumber, amount);
		return Response.ok().build();
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
}
