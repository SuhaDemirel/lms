package com.bank.loanapi.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultResponse {
    private Integer installmentsPaid;
    private BigDecimal totalAmountSpent;
    private Boolean isLoanFullyPaid;
    private BigDecimal remainingLoanAmount;
    private List<InstallmentPaymentDetail> paidInstallments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstallmentPaymentDetail {
        private Long installmentId;
        private BigDecimal originalAmount;
        private BigDecimal paidAmount;
        private BigDecimal discountOrPenalty;
        private String paymentType; // EARLY, ON_TIME, LATE
    }
}
