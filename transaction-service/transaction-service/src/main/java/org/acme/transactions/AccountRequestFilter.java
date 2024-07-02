package org.acme.transactions;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

public class AccountRequestFilter implements ClientRequestFilter{

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		String invokedMethod = (String) requestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
		requestContext.getHeaders().add("Invoked-Client-Method", invokedMethod);
		
	}
	
}
