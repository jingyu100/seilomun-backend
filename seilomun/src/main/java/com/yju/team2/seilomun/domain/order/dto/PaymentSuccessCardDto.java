package com.yju.team2.seilomun.domain.order.dto;

import lombok.Data;

@Data
public class PaymentSuccessCardDto {
    String company;
    String number;
    String installmentPlanMonths;
    String isInterestFree;
    String approveNo;
    String useCardPoint;
    String cardType;
    String ownerType;
    String acquiresStatus;
    String receiptUrl;
}
