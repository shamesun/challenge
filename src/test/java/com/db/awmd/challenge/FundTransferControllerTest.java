package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.FundService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class FundTransferControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private FundService fundService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        fundService.getAccountService().getAccountsRepository().clearAccounts();
        Account account1 = new Account("Id-123");
        account1.setBalance(new BigDecimal(500));
        fundService.getAccountService().createAccount(account1);

        Account account2 = new Account("Id-234");
        account2.setBalance(new BigDecimal(100));
        fundService.getAccountService().createAccount(account2);
    }

    /*
      Test Case For Fund Transfer
       */
    @Test
    public void fundTransferNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransferBlankBody() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransferEmptySenderAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"senderAccountId\":\"\",\"receiverAccountId\":\"Id-234\",\"fund\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransferEmptyReceiverAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"senderAccountId\":\"Id-123\",\"receiverAccountId\":\"\",\"fund\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransferNoSenderAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiverAccountId\":\"Id-234\",\"fund\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransferNoReceiverAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"senderAccountId\":\"Id-123\",\"fund\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransferNoFund() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"senderAccountId\":\"Id-123\",\"receiverAccountId\":\"Id-234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransferNegativefund() throws Exception {
        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"senderAccountId\":\"Id-123\",\"receiverAccountId\":\"Id-234\",\"fund\":-1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fundTransfer_valid() throws Exception {
        String validJson = "{\"senderAccountId\":\"Id-123\",\"receiverAccountId\":\"Id-234\",\"fund\":400}";

        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/v1/accounts/Id-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"accountId\":\"Id-123\",\"balance\":100}"));

        this.mockMvc.perform(get("/v1/accounts/Id-234"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"accountId\":\"Id-234\",\"balance\":500}"));
    }


    @Test
    public void fundTransfer_OverDraftNotSupported() throws Exception {

        String validJson = "{\"senderAccountId\":\"Id-123\",\"receiverAccountId\":\"Id-234\",\"fund\":600}";

        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isNotAcceptable());

        //verifying the fund is not transferred
        this.mockMvc.perform(get("/v1/accounts/Id-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"accountId\":\"Id-123\",\"balance\":500}"));

        this.mockMvc.perform(get("/v1/accounts/Id-234"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"accountId\":\"Id-234\",\"balance\":100}"));
    }

    @Test
    public void fundTransferWhenSenderAcNotExist() throws Exception {
        String validJson = "{\"senderAccountId\":\"Id-456\",\"receiverAccountId\":\"Id-234\",\"fund\":300}";

        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Debiting Account with accountId Id-456 does not available in system"));
    }

    @Test
    public void fundTransferWhenReceiverAcNotExist() throws Exception {
        String validJson = "{\"senderAccountId\":\"Id-123\",\"receiverAccountId\":\"Id-456\",\"fund\":300}";

        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Crediting Account with accountId Id-456 does not available in system"));
    }

    @Test
    public void fundTransferInSameAccount() throws Exception {
        String validJson = "{\"senderAccountId\":\"Id-123\",\"receiverAccountId\":\"Id-123\",\"fund\":300}";

        this.mockMvc.perform(post("/v1/fund/transfer").contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().string("Sender and Receiver can't be same"));
    }

}
