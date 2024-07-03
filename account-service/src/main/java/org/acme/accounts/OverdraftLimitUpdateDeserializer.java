package org.acme.accounts;

import org.acme.accounts.events.OverdraftLimitUpdate;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;

public class OverdraftLimitUpdateDeserializer extends JsonbDeserializer<OverdraftLimitUpdate> {

	public OverdraftLimitUpdateDeserializer() {
		super(OverdraftLimitUpdate.class);
	}
	
}
