package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.FundTransfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.InvalidTransactionException;
import com.db.awmd.challenge.exception.OverDraftNotSuportedException;
import com.db.awmd.challenge.service.FundService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FundServiceTest {


    @Mock
    private NotificationService notificationService;
    @Autowired
    private FundService fundService;

    //------------- Fund transfer test cases
    @Before
    public void initAccounts() {
        fundService.getAccountService().getAccountsRepository().clearAccounts();
        MockitoAnnotations.initMocks(this);

        Account account1 = new Account("Id-123");
        account1.setBalance(new BigDecimal(500));
        fundService.getAccountService().createAccount(account1);

        Account account2 = new Account("Id-234");
        account2.setBalance(new BigDecimal(100));
        fundService.getAccountService().createAccount(account2);
    }

    @Test
    public void fundTransfer() throws RuntimeException {
        Mockito.doAnswer(
                invocation -> {
                    Object[] args = invocation.getArguments();
                    //verifying notification sent only in case of valid transaction
                    assertThat(((Account) args[0]).getBalance()).isPositive();
                    return args;
                }
        ).when(notificationService).notifyAboutTransfer(any(Account.class), anyString());
        FundTransfer fundTransfer = new FundTransfer("Id-123", "Id-234");
        fundTransfer.setFund(new BigDecimal(400));
        this.fundService.fundTransfer(fundTransfer);

        assertThat(fundService.getAccountService().getAccount("Id-123").getBalance()).isEqualTo(new BigDecimal(100));
        assertThat(fundService.getAccountService().getAccount("Id-234").getBalance()).isEqualTo(new BigDecimal(500));
    }

    @Test
    public void fundTransfer_ZeroFundNotSupported() throws RuntimeException {
        FundTransfer fundTransfer = new FundTransfer("Id-123", "Id-234");
        fundTransfer.setFund(new BigDecimal(0));
        try {
            this.fundService.fundTransfer(fundTransfer);
            fail("Should have failed when transferring 0 fund");
        } catch (InvalidTransactionException ite) {
            assertThat(ite.getMessage()).isEqualTo("Only positive fund transfer supported in system");
        }
        //verifying the fund is not transferred
        assertThat(fundService.getAccountService().getAccount("Id-123").getBalance()).isEqualTo(new BigDecimal(500));
        assertThat(fundService.getAccountService().getAccount("Id-234").getBalance()).isEqualTo(new BigDecimal(100));
    }

    @Test
    public void fundTransfer_NegativeFundNotSupported() throws RuntimeException {
        FundTransfer fundTransfer = new FundTransfer("Id-123", "Id-234");
        fundTransfer.setFund(new BigDecimal(-100));
        try {
            this.fundService.fundTransfer(fundTransfer);
            fail("Should have failed when transferring -ive fund");
        } catch (InvalidTransactionException ite) {
            assertThat(ite.getMessage()).isEqualTo("Only positive fund transfer supported in system");
        }
        //verifying the fund is not transferred
        assertThat(fundService.getAccountService().getAccount("Id-123").getBalance()).isEqualTo(new BigDecimal(500));
        assertThat(fundService.getAccountService().getAccount("Id-234").getBalance()).isEqualTo(new BigDecimal(100));
    }

    @Test
    public void fundTransfer_OverDraftNotSupported() throws RuntimeException {
        FundTransfer fundTransfer = new FundTransfer("Id-123", "Id-234");
        fundTransfer.setFund(new BigDecimal(600));
        try {
            this.fundService.fundTransfer(fundTransfer);
            fail("Should have failed when transferring fund than available");
        } catch (OverDraftNotSuportedException odnse) {
            assertThat(odnse.getMessage()).isEqualTo("The Debiting Fund 600 from AccountId Id-123 is Not allowed, Due to less balance");
        }
        //verifying the fund is not transferred
        assertThat(fundService.getAccountService().getAccount("Id-123").getBalance()).isEqualTo(new BigDecimal(500));
        assertThat(fundService.getAccountService().getAccount("Id-234").getBalance()).isEqualTo(new BigDecimal(100));
    }

    @Test
    public void fundTransferWhenSenderAcNotExist() throws RuntimeException {
        FundTransfer fundTransfer = new FundTransfer("Id-456", "Id-234");
        fundTransfer.setFund(new BigDecimal(400));
        try {
            this.fundService.fundTransfer(fundTransfer);
            fail("Should have failed when un-available Account id");
        } catch (AccountNotFoundException anfe) {
            assertThat(anfe.getMessage()).isEqualTo("Debiting Account with accountId Id-456 does not available in system");
        }
    }

    @Test
    public void fundTransferWhenReceiverAcNotExist() throws RuntimeException {
        FundTransfer fundTransfer = new FundTransfer("Id-234", "Id-456");
        fundTransfer.setFund(new BigDecimal(400));
        try {
            this.fundService.fundTransfer(fundTransfer);
            fail("Should have failed when un-available Account id");
        } catch (AccountNotFoundException anfe) {
            assertThat(anfe.getMessage()).isEqualTo("Crediting Account with accountId Id-456 does not available in system");
        }
    }

}
