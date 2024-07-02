package org.acme.accounts;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Date;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@ApplicationScoped
@Liveness
public class AlwaysHealthyLivenessCheck implements HealthCheck{

	@Override
	public HealthCheckResponse call() {
		return HealthCheckResponse
				.named("Always live")
				.withData("time", String.valueOf(new Date()))
				.up()
				.build();
	}
	
}
