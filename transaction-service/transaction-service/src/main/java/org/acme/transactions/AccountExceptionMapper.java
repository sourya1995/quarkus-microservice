package org.acme.transactions;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class AccountExceptionMapper implements ResponseExceptionMapper<AccountNotFoundException> {

	@Override
	public AccountNotFoundException toThrowable(Response response) {
		return new AccountNotFoundException("Failed to retrieve account");
	}

	@Override
	public boolean handles(int status, MultivaluedMap<String, Object> headers) {
		return status == 404;
	}

}
