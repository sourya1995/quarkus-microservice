package org.acme.accounts;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Account {
	@Id
	@GeneratedValue
	private Long id;

	private Long accountNumber;
	private Long customerNumber;
	private String customerName;
	private BigDecimal balance;
	private AccountStatus accountStatus = AccountStatus.OPEN;

	public void markOverdrawn() {
		accountStatus = AccountStatus.OVERDRAWN;
	}

	public void removeOverdrawnStatus() {
		accountStatus = AccountStatus.OPEN;
	}

	public void close() {
		accountStatus = AccountStatus.CLOSED;
		balance = BigDecimal.valueOf(0);
	}

	public void withdrawFunds(BigDecimal amount) {
		balance = balance.subtract(amount);
	}

	public void addFunds(BigDecimal amount) {
		balance = balance.add(amount);
	}

	public Long getAccountNumber() {
		return accountNumber;
	}

	public String getCustomerName() {
		return customerName;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setAccountNumber(Long accountNumber) {
		this.accountNumber = accountNumber;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public Long getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(Long customerNumber) {
		this.customerNumber = customerNumber;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accountNumber, accountStatus, balance, customerName, customerNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Account other = (Account) obj;
		return Objects.equals(accountNumber, other.accountNumber) && accountStatus == other.accountStatus
				&& Objects.equals(balance, other.balance) && Objects.equals(customerName, other.customerName)
				&& Objects.equals(customerNumber, other.customerNumber);
	}

}
