package com.bank.loanapi.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private BigDecimal loanAmount;
    private Integer numberOfInstallments;
    private BigDecimal interestRate;
    private LocalDateTime createDate;
    private Boolean isPaid;
    private BigDecimal totalAmount;
    private Integer paidInstallments;
    private Integer remainingInstallments;
}
