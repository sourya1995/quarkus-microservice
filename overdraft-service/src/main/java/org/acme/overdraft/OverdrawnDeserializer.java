package org.acme.overdraft;

import org.acme.overdraft.events.Overdrawn;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;

public class OverdrawnDeserializer extends JsonbDeserializer<Overdrawn>{
	public OverdrawnDeserializer() {
	    super(Overdrawn.class);
	  }
}
