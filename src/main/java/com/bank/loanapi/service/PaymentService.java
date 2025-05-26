package com.bank.loanapi.service;

import com.bank.loanapi.dto.request.PayLoanRequest;
import com.bank.loanapi.dto.response.PaymentResultResponse;
import com.bank.loanapi.dto.response.PaymentResultResponse.InstallmentPaymentDetail;
import com.bank.loanapi.entity.*;
import com.bank.loanapi.exception.ResourceNotFoundException;
import com.bank.loanapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final CustomerRepository customerRepository;
    private final LoanService loanService;

    private static final BigDecimal EARLY_PAYMENT_DISCOUNT_RATE = new BigDecimal("0.001");
    private static final BigDecimal LATE_PAYMENT_PENALTY_RATE = new BigDecimal("0.001");
    private static final int MAX_PAYABLE_MONTHS_AHEAD = 3;

    public PaymentResultResponse payLoan(Long loanId, PayLoanRequest request) {
        Loan loan = loanRepository.findByIdWithInstallments(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        Customer customer = loan.getCustomer();
        loanService.checkCustomerAccess(customer);

        if (loan.getIsPaid()) {
            throw new IllegalStateException("Loan is already fully paid");
        }

        LocalDate today = LocalDate.now();
        LocalDate maxPayableDate = today.plusMonths(MAX_PAYABLE_MONTHS_AHEAD).withDayOfMonth(1);

        List<LoanInstallment> payableInstallments = installmentRepository
                .findPayableInstallments(loanId, maxPayableDate);

        if (payableInstallments.isEmpty()) {
            throw new IllegalStateException("No payable installments found");
        }

        BigDecimal remainingAmount = request.getAmount();
        List<InstallmentPaymentDetail> paidInstallmentDetails = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (LoanInstallment installment : payableInstallments) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal requiredAmount = calculateRequiredAmount(installment, today);

            if (remainingAmount.compareTo(requiredAmount) >= 0) {
                installment.setPaidAmount(requiredAmount);
                installment.setPaymentDate(today);
                installment.setIsPaid(true);

                remainingAmount = remainingAmount.subtract(requiredAmount);
                totalSpent = totalSpent.add(requiredAmount);

                String paymentType = determinePaymentType(installment.getDueDate(), today);
                BigDecimal discountOrPenalty = requiredAmount.subtract(installment.getAmount());

                paidInstallmentDetails.add(InstallmentPaymentDetail.builder()
                        .installmentId(installment.getId())
                        .originalAmount(installment.getAmount())
                        .paidAmount(requiredAmount)
                        .discountOrPenalty(discountOrPenalty)
                        .paymentType(paymentType)
                        .build());
            }
        }

        boolean allPaid = loan.getInstallments().stream().allMatch(LoanInstallment::getIsPaid);
        if (allPaid) {
            loan.setIsPaid(true);
            customer.setUsedCreditLimit(
                    customer.getUsedCreditLimit().subtract(
                            loan.getLoanAmount().multiply(BigDecimal.ONE.add(loan.getInterestRate()))
                    )
            );
            customerRepository.save(customer);
        }

        loanRepository.save(loan);

        BigDecimal remainingLoanAmount = loan.getInstallments().stream()
                .filter(i -> !i.getIsPaid())
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentResultResponse.builder()
                .installmentsPaid(paidInstallmentDetails.size())
                .totalAmountSpent(totalSpent)
                .isLoanFullyPaid(allPaid)
                .remainingLoanAmount(remainingLoanAmount)
                .paidInstallments(paidInstallmentDetails)
                .build();
    }

    private BigDecimal calculateRequiredAmount(LoanInstallment installment, LocalDate paymentDate) {
        long daysDifference = ChronoUnit.DAYS.between(paymentDate, installment.getDueDate());

        if (daysDifference > 0) {
            BigDecimal discount = installment.getAmount()
                    .multiply(EARLY_PAYMENT_DISCOUNT_RATE)
                    .multiply(new BigDecimal(daysDifference))
                    .setScale(2, RoundingMode.HALF_UP);
            return installment.getAmount().subtract(discount);
        } else if (daysDifference < 0) {
            BigDecimal penalty = installment.getAmount()
                    .multiply(LATE_PAYMENT_PENALTY_RATE)
                    .multiply(new BigDecimal(Math.abs(daysDifference)))
                    .setScale(2, RoundingMode.HALF_UP);
            return installment.getAmount().add(penalty);
        } else {
            return installment.getAmount();
        }
    }

    private String determinePaymentType(LocalDate dueDate, LocalDate paymentDate) {
        long daysDifference = ChronoUnit.DAYS.between(paymentDate, dueDate);
        if (daysDifference > 0) {
            return "EARLY";
        } else if (daysDifference < 0) {
            return "LATE";
        } else {
            return "ON_TIME";
        }
    }
}