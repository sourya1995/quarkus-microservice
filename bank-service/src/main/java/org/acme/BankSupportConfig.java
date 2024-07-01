package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix="bank-support")
public class BankSupportConfig {
	private String phone;
	
	public String email;

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	
}
