package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.FundTransfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.OverDraftNotSuportedException;
import com.db.awmd.challenge.service.FundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/fund")
@Slf4j
public class FundTransferController {

    private final FundService fundService;

    @Autowired
    public FundTransferController(FundService fundService) {
        this.fundService = fundService;
    }

    @PostMapping(path = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> fundTransfer(@RequestBody @Valid FundTransfer fundTransfer) {
        log.info("transferring fund {}", fundTransfer);
        try {
            if (fundTransfer.getSenderAccountId().equalsIgnoreCase(fundTransfer.getReceiverAccountId())) {
                return new ResponseEntity<>("Sender and Receiver can't be same", HttpStatus.NOT_ACCEPTABLE);
            }
            this.fundService.fundTransfer(fundTransfer);
        } catch (AccountNotFoundException anfe) {
            return new ResponseEntity<>(anfe.getMessage(), HttpStatus.NOT_FOUND);
        } catch (OverDraftNotSuportedException onse) {
            return new ResponseEntity<>(onse.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
