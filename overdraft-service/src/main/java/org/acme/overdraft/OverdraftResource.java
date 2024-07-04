package org.acme.overdraft;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.acme.overdraft.events.OverdraftLimitUpdate;
import org.acme.overdraft.events.Overdrawn;
import org.acme.overdraft.model.AccountOverdraft;
import org.acme.overdraft.model.CustomerOverdraft;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.jaegertracing.thriftjava.Span;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public class OverdraftResource {

	private final Map<Long, CustomerOverdraft> customerOverdrafts = new HashMap<>();

	@Inject
	Tracer tracer;

	@Incoming("account-overdrawn")
	@Outgoing("customer-overdrafts")
	public Message<Overdrawn> overdraftNotification(Message<Overdrawn> message) {
		Overdrawn overdrawnPayload = message.getPayload();
		CustomerOverdraft customerOverdraft = customerOverdrafts.get(overdrawnPayload.accountNumber);
		if (customerOverdraft == null) {
			customerOverdraft = new CustomerOverdraft();
			customerOverdraft.customerNumber = overdrawnPayload.customerNumber;

			customerOverdrafts.put(overdrawnPayload.customerNumber, customerOverdraft);
		}
		AccountOverdraft accountOverdraft = customerOverdraft.accountOverdrafts.get(overdrawnPayload.accountNumber);

		if (accountOverdraft == null) {
			accountOverdraft = new AccountOverdraft();
			accountOverdraft.accountNumber = overdrawnPayload.accountNumber;

			customerOverdraft.accountOverdrafts.put(overdrawnPayload.accountNumber, accountOverdraft);
		}
		customerOverdraft.totalOverdrawnEvents++;
		accountOverdraft.currentOverdraft = overdrawnPayload.overdraftLimit;
		accountOverdraft.numberOverdrawnEvents++;

		RecordHeaders headers = new RecordHeaders();
		if (message.getMetadata(IncomingKafkaRecordMetadata.class).isPresent()) {
			Span span = (Span) tracer.buildSpan("process-overdraft-fee").asChildOf(TracingKafkaUtils.extractSpanContext(
					message.getMetadata(IncomingKafkaRecordMetadata.class).get().getHeaders(), tracer)).start();
			try (Scope scope = tracer.activateSpan((io.opentracing.Span) span)) {
				TracingKafkaUtils.inject(((io.opentracing.Span) span).context(), headers, tracer);
			} finally {
				((io.opentracing.Span) span).finish();
			}
		}
		OutgoingKafkaRecordMetadata<Object> kafkaMetadata = OutgoingKafkaRecordMetadata.builder().withHeaders(headers)
				.build();

		return message.addMetadata(customerOverdraft).addMetadata(kafkaMetadata);

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	public List<AccountOverdraft> retrieveAllAccountOverdrafts() {
		return customerOverdrafts.values().stream().flatMap(e -> e.accountOverdrafts.values().stream())
				.collect(Collectors.toList());
	}

	@Inject
	@Channel("overdraft-update")
	Emitter<OverdraftLimitUpdate> emitter;

	@PUT
	@Path("/{accountNumber}")
	public void updateAccountOverdraft(@PathParam("accountNumber") Long accountNumber, BigDecimal amount) {
		OverdraftLimitUpdate updateEvent = new OverdraftLimitUpdate();
		updateEvent.accountNumber = accountNumber;
		updateEvent.newOverdraftLimit = amount;

		emitter.send(updateEvent);
	}

}
