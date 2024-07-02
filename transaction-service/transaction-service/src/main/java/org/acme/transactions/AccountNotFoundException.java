package org.acme.transactions;

public class AccountNotFoundException extends Exception {

	public AccountNotFoundException(String message) {
		super(message);
	}

}
