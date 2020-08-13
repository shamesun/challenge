package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.FundTransfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InvalidTransactionException;
import com.db.awmd.challenge.exception.OverDraftNotSuportedException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
              "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) {
    return accounts.get(accountId);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }

  @Override
  public boolean transferFund(FundTransfer fundTransfer) {
    if (accounts.get(fundTransfer.getSenderAccountId()) == null)
      throw new AccountNotFoundException("Debiting Account with accountId " + fundTransfer.getSenderAccountId() + " does not available in system");
    if (accounts.get(fundTransfer.getReceiverAccountId()) == null)
      throw new AccountNotFoundException("Crediting Account with accountId " + fundTransfer.getReceiverAccountId() + " does not available in system");
    if (fundTransfer.getFund().compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTransactionException("Only positive fund transfer supported in system");
    }
    Account debitingAc;
    synchronized (accounts.get(fundTransfer.getSenderAccountId())) {
      if (accounts.get(fundTransfer.getSenderAccountId()).getBalance().subtract(fundTransfer.getFund()).compareTo(BigDecimal.ZERO) < 1)
        throw new OverDraftNotSuportedException(
                "The Debiting Fund " + fundTransfer.getFund() + " from AccountId " + fundTransfer.getSenderAccountId() + " is Not allowed," +
                        " Due to less balance"
        );
      debitingAc = accounts.get(fundTransfer.getSenderAccountId());
      debitingAc.setBalance(debitingAc.getBalance().subtract(fundTransfer.getFund()));
      accounts.put(fundTransfer.getSenderAccountId(), debitingAc);
    }

    synchronized (accounts.get(fundTransfer.getReceiverAccountId())) {
      Account credtttingAc = accounts.get(fundTransfer.getReceiverAccountId());
      credtttingAc.setBalance(credtttingAc.getBalance().add(fundTransfer.getFund()));
      accounts.put(fundTransfer.getReceiverAccountId(), credtttingAc);
    }
    return true;

  }
}
