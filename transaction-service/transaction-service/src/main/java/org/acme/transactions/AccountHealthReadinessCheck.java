package org.acme.transactions;

import java.math.BigDecimal;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@Readiness
public class AccountHealthReadinessCheck implements HealthCheck {
	@Inject
	@RestClient
	AccountService accountService;

	BigDecimal balance;

	@Override
	public HealthCheckResponse call() {
		try {
			balance = accountService.getBalance(999999999L);

		} catch (WebApplicationException ex) {
			balance = new BigDecimal(Integer.MIN_VALUE);
			if (ex.getResponse().getStatus() >= 500) {
				return HealthCheckResponse.named("AccountServiceCheck").withData("exception", ex.toString()).down()
						.build();
			}
		}

		return HealthCheckResponse.named("AccountServiceCheck").withData("balance", balance.toString()).up().build();

	}

}
