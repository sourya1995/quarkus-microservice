package org.acme.accounts;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Path("/accounts")
public class AccountResource {
	Set<Account> accounts = new HashSet<>();
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Set<Account> allAccounts(){
		return accounts;
	}
	
	@PostConstruct
	public void setup() {
		accounts.add(new Account(123456789L, 987654321L, "George Baird", new BigDecimal("354.23")));
	    accounts.add(new Account(121212121L, 888777666L, "Mary Taylor", new BigDecimal("560.03")));
	    accounts.add(new Account(545454545L, 222444999L, "Diana Rigg", new BigDecimal("422.00")));
	}
	
	@GET
	@Path("/{accountNumber}")
	@Produces(MediaType.APPLICATION_JSON)
	public Account getAccount(@PathParam("accountNumber") Long accountNumber) {
		Optional<Account> response = accounts.stream().filter(acct -> acct.getAccountNumber().equals(accountNumber)).findFirst();
		
		return response.orElseThrow(() -> new WebApplicationException("Account with id of " + accountNumber + "does not exist"));
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createAccount(Account account) {
		if(account.getAccountNumber() == null) {
			throw new WebApplicationException("No Account Number specified", 400);
		}
		
		accounts.add(account);
		return Response.status(201).entity(account).build();
	}
	
	@PUT
	@Path("{accountNumber}/withdrawal")
	public Account withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
		Account account = getAccount(accountNumber);
		account.withdrawFunds(new BigDecimal(amount));
		return account;
	}
	
	@PUT
	@Path("{accountNumber}/deposit")
	public Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) {
		Account account = getAccount(accountNumber);
		account.addFunds(new BigDecimal(amount));
		return account;
	}
	
	@DELETE
	@Path("{accountNumber}")
	public Response closeAccount(@PathParam("accountNumber") Long accountNumber) {
		Account oldAccount = getAccount(accountNumber);
		accounts.remove(oldAccount);
		return Response.noContent().build();
	}
	
	@Provider
	public static class ErrorMapper implements ExceptionMapper<Exception> {

		@Override
		public Response toResponse(Exception exception) {
			int code = 500;
			if(exception instanceof WebApplicationException) {
				code = ((WebApplicationException) exception).getResponse().getStatus();
			}
			
			JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
					.add("exceptionType", exception.getClass().getName())
					.add("code", code);
			
			if(exception.getMessage() != null) {
				entityBuilder.add("error", exception.getMessage());
			}
			
			return Response.status(code)
					.entity(entityBuilder.build())
					.build();
		}
		
	}
}
