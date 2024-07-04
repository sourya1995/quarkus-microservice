package org.acme.transactions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@Path("/accounts")
@RegisterRestClient
@ClientHeaderParam(name = "class-level-param", value = "Account-Service interface")
@RegisterClientHeaders
@RegisterProvider(AccountRequestFilter.class)
@RegisterProvider(AccountExceptionMapper.class)
@Produces(MediaType.APPLICATION_JSON)
public interface AccountService {
	@GET
	@Path("/{accountNumber}/balance")
	BigDecimal getBalance(@PathParam("accountNumber") Long accountNumber);

	@GET
	@Path("/jwt-secure/{acctNumber}/balance")
	BigDecimal getBalanceSecure(@PathParam("acctNumber") Long accountNumber);

	@POST
	@Path("{accountNumber}/transaction")
	Map<String, List<String>> transact(@PathParam("accountNumber") Long accountNumber, BigDecimal amount)
			throws AccountNotFoundException;

	@POST
	@Path("{accountNumber}/transaction")
	@ClientHeaderParam(name = "method-level-param", value = "{generateValue}")
	CompletionStage<Map<String, List<String>>> transactAsync(@PathParam("accountNumber") Long accountNumber,
			BigDecimal amount);

	default String generateValue() {
		return "Value generated in method for async call";
	}
}
