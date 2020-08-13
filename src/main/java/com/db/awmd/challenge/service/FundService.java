package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.FundTransfer;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FundService {
    @Getter
    private final AccountsService accountService;

    @Getter
    private final NotificationService notificationService;

    @Autowired
    public FundService(AccountsService accountService, NotificationService notificationService) {
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    public boolean fundTransfer(FundTransfer fundTransfer) {
        boolean transferStatus = this.accountService.getAccountsRepository().transferFund(fundTransfer);
        if (transferStatus) {
            this.getNotificationService().notifyAboutTransfer(this.accountService.getAccount(
                    fundTransfer.getSenderAccountId()),
                    " Account debited with " + fundTransfer.getFund());
            this.getNotificationService().notifyAboutTransfer(this.accountService.getAccount(
                    fundTransfer.getReceiverAccountId()),
                    " Account Credited with " + fundTransfer.getFund());
        }
        return transferStatus;
    }
}
