package org.acme.accounts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.acme.accounts.events.OverdraftLimitUpdate;
import org.acme.accounts.events.Overdrawn;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.annotation.security.RolesAllowed;
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

	@RolesAllowed("customer")
	@GET
	@Path("/jwt-secure/{acctNumber}/balance")
	public BigDecimal getBalanceJWT(@PathParam("acctNumber") Long accountNumber) {
		return getBalance(accountNumber);
	}

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
	public Map<String, List<String>> transact(@Context HttpHeaders headers,
			@PathParam("accountNumber") Long accountNumber, BigDecimal amount) {
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

	@Inject
	@Channel("account-overdrawn")
	Emitter<Overdrawn> emitter;

	int ackedMessages = 0;
	List<Throwable> failures = new ArrayList<>();

	@Inject
	Tracer tracer;

	@SuppressWarnings("unchecked")
	@PUT
	@Path("{accountNumber}/withdrawal")
	@Traced(operationName = "withdraw-from-account")
	@Transactional
	public CompletionStage<Account> withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
		Account entity = accountRepository.findByAccountNumber(accountNumber);
		if (entity == null) {
			throw new WebApplicationException("Account with " + accountNumber + " does not exist.", 404);
		}

		if (entity.getAccountStatus().equals(AccountStatus.OVERDRAWN)
				&& entity.getBalance().compareTo(entity.getOverdraftLimit()) <= 0) {
			throw new WebApplicationException("Account is overdrawn, no further withdrawals permitted", 409);
		}

		entity.withdrawFunds(new BigDecimal(amount));
		tracer.activeSpan().setTag("accountNumber", accountNumber);
		tracer.activeSpan().setBaggageItem("withdrawalAmount", amount);
		if (entity.getBalance().compareTo(BigDecimal.ZERO) < 0) {
			entity.markOverdrawn();
			accountRepository.persist(entity);
			Overdrawn payload = new Overdrawn(entity.getAccountNumber(), entity.getCustomerNumber(),
					entity.getBalance(), entity.getOverdraftLimit());
//		payload only ->	return emitter.send(payload).thenCompose(empty -> CompletableFuture.completedFuture(entity)); 
			RecordHeaders headers = new RecordHeaders();
			TracingKafkaUtils.inject(tracer.activeSpan().context(), headers, tracer);
			OutgoingKafkaRecordMetadata<Object> kafkaMetadata = OutgoingKafkaRecordMetadata.builder()
					.withHeaders(headers).build();

			CompletableFuture<Account> future = new CompletableFuture<>();
			emitter.send(Message.of(payload, () -> {
				ackedMessages++;
				future.complete(entity);
				return CompletableFuture.completedFuture(null);
			}, reason -> {
				failures.add(reason);
				return CompletableFuture.completedFuture(null);
			}));
			return future;
		}

		accountRepository.persist(entity);
		return CompletableFuture.completedFuture(entity);
	}

	@Incoming("overdraft-update") // will use another thread
	@Blocking
	@Transactional
	public void processOverdraftUpdate(OverdraftLimitUpdate overdraftLimitUpdate) {
		Account account = accountRepository.findByAccountNumber(overdraftLimitUpdate.accountNumber);
		account.setOverdraftLimit(overdraftLimitUpdate.newOverdraftLimit);
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
		@Metric(name = "ErrorMapperCounter", description = "Number of times the AccountResource ErrorMapper is invoked")
		Counter errorMapperCounter;

		@Override
		public Response toResponse(Exception exception) {
			errorMapperCounter.inc();
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
