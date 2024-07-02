package org.acme.transactions;

import java.math.BigDecimal;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)

public interface AccountServiceProgrammatic {
	@GET
	@Path("/{accountNumber}/balance")
	BigDecimal getBalance(@PathParam("accountNumber") Long accountNumber);
	
	@POST
	@Path("{accountNumber}/transaction")
	void transact(@PathParam("accountNumber") Long accountNumber, BigDecimal amount);
	
	@POST
	@Path("{accountNumber}/transaction")
	CompletionStage<Void> transactAsync(@PathParam("accountNumber") Long accountNumber, BigDecimal amount);
}

