package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FundTransfer {

    @NotNull
    @NotEmpty
    private final String senderAccountId;

    @NotNull
    @NotEmpty
    private final String receiverAccountId;

    @NotNull
    @Min(value = 0, message = "fund must be positive.")
    private BigDecimal fund;

    /**
     * this constructor just provided for default behaviour
     *
     * @param senderAccountId   : the account sendi fund
     * @param receiverAccountId : The fund fund receiving account
     */
    public FundTransfer(String senderAccountId, String receiverAccountId) {
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.fund = BigDecimal.ZERO;
    }

    @JsonCreator
    public FundTransfer(@JsonProperty("sender") String senderAccountId,
                        @JsonProperty("receiver") String receiverAccountId,
                        @JsonProperty("fund") BigDecimal fund) {
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.fund = fund;
    }
}
