package org.acme.accounts;

import java.math.BigDecimal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

	@Inject
	AccountRepository accountRepository;

	@GET
	public List<Account> allAccounts() {
		return accountRepository.listAll();
	}

	@GET
	@Path("/{accountNumber}")
	public Account getAccount(@PathParam("accountNumber") Long accountNumber) {
		Account account = accountRepository.findByAccountNumber(accountNumber);

		if (accountNumber == null) {
			throw new WebApplicationException("Account with" + accountNumber + "does not exist", 404);
		}

		return account;
	}
	
	@GET
	@Path("/{accountNumber}/balance")
	public BigDecimal getBalance(@PathParam("accountNumber") Long accountNumber) {
		Account account = accountRepository.findByAccountNumber(accountNumber);
		
		if (account == null) {
			throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
		}
		
		return account.getBalance();
		
	}
	
	@POST
	  @Path("{accountNumber}/transaction")
	  @Transactional
	  public Map<String, List<String>> transact(@Context HttpHeaders headers, @PathParam("accountNumber") Long accountNumber, BigDecimal amount) {
	    Account entity = accountRepository.findByAccountNumber(accountNumber);

	    if (entity == null) {
	      throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
	    }

	    if (entity.getAccountStatus().equals(AccountStatus.OVERDRAWN)) {
	      throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
	    }

	    entity.setBalance(entity.addFunds(amount)); 
	    return headers.getRequestHeaders();
	  }

	@POST
	@Transactional
	public Response createAccount(Account account) {
		if (account.getId() == null) {
			throw new WebApplicationException("Invalid ID", 400);
		}

		accountRepository.persist(account);
		return Response.status(201).entity(account).build();
	}

	@PUT
	@Path("{accountNumber}/withdrawal")
	@Transactional
	public Account withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
		Account entity = accountRepository.findByAccountNumber(accountNumber);
		if (entity == null) {
			throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
		}

		if (entity.getAccountStatus().equals(AccountStatus.OVERDRAWN)) {
			throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
		}

		entity.withdrawFunds(new BigDecimal(amount));
		return entity;
	}

	@PUT
	@Path("{accountNumber}/deposit")
	@Transactional
	public Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) {
		Account entity = accountRepository.findByAccountNumber(accountNumber);
		if (entity == null) {
			throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
		}

		entity.addFunds(new BigDecimal(amount));
		return entity;
	}

	@DELETE
	@Path("{accountNumber}")
	public Response closeAccount(@PathParam("accountNumber") Long accountNumber) {
		Account entity = accountRepository.findByAccountNumber(accountNumber);
		if (entity == null) {
			throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
		}

		entity.close();
		return Response.noContent().build();
	}

	@Provider
	public static class ErrorMapper implements ExceptionMapper<Exception> {

		@Override
		public Response toResponse(Exception exception) {
			int code = 500;
			if (exception instanceof WebApplicationException) {
				code = ((WebApplicationException) exception).getResponse().getStatus();
			}

			JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
					.add("exceptionType", exception.getClass().getName()).add("code", code);

			if (exception.getMessage() != null) {
				entityBuilder.add("error", exception.getMessage());
			}

			return Response.status(code).entity(entityBuilder.build()).build();
		}

	}
}
