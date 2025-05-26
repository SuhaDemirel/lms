package com.bank.loanapi.service;

import com.bank.loanapi.dto.request.CreateLoanRequest;
import com.bank.loanapi.dto.response.InstallmentResponse;
import com.bank.loanapi.dto.response.LoanResponse;
import com.bank.loanapi.entity.*;
import com.bank.loanapi.exception.*;
import com.bank.loanapi.repository.*;
import com.bank.loanapi.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final LoanInstallmentRepository installmentRepository;

    private static final Set<Integer> ALLOWED_INSTALLMENTS = Set.of(6, 9, 12, 24);

    public LoanResponse createLoan(CreateLoanRequest request) {
        if (!ALLOWED_INSTALLMENTS.contains(request.getNumberOfInstallments())) {
            throw new InvalidLoanParametersException("Number of installments must be 6, 9, 12, or 24");
        }

        if (request.getInterestRate().compareTo(new BigDecimal("0.1")) < 0 ||
                request.getInterestRate().compareTo(new BigDecimal("0.5")) > 0) {
            throw new InvalidLoanParametersException("Interest rate must be between 0.1 and 0.5");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        checkCustomerAccess(customer);

        BigDecimal totalAmount = request.getAmount()
                .multiply(BigDecimal.ONE.add(request.getInterestRate()))
                .setScale(2, RoundingMode.HALF_UP);

        if (customer.getAvailableCreditLimit().compareTo(totalAmount) < 0) {
            throw new InsufficientCreditException(
                    String.format("Insufficient credit limit. Available: %s, Required: %s",
                            customer.getAvailableCreditLimit(), totalAmount));
        }

        Loan loan = Loan.builder()
                .customer(customer)
                .loanAmount(request.getAmount())
                .numberOfInstallment(request.getNumberOfInstallments())
                .interestRate(request.getInterestRate())
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .installments(new ArrayList<>())
                .build();

        BigDecimal installmentAmount = totalAmount
                .divide(new BigDecimal(request.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);

        LocalDate firstDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        for (int i = 0; i < request.getNumberOfInstallments(); i++) {
            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .amount(installmentAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(firstDueDate.plusMonths(i))
                    .isPaid(false)
                    .build();
            loan.getInstallments().add(installment);
        }

        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(totalAmount));
        customerRepository.save(customer);

        Loan savedLoan = loanRepository.save(loan);

        return mapToLoanResponse(savedLoan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> listLoans(Long customerId, Integer numberOfInstallments, Boolean isPaid) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        checkCustomerAccess(customer);

        List<Loan> loans;

        if (numberOfInstallments != null && isPaid != null) {
            loans = loanRepository.findByCustomerIdAndNumberOfInstallments(customerId, numberOfInstallments)
                    .stream()
                    .filter(loan -> loan.getIsPaid().equals(isPaid))
                    .collect(Collectors.toList());
        } else if (isPaid != null) {
            loans = loanRepository.findByCustomerIdAndIsPaid(customerId, isPaid);
        } else if (numberOfInstallments != null) {
            loans = loanRepository.findByCustomerIdAndNumberOfInstallments(customerId, numberOfInstallments);
        } else {
            loans = loanRepository.findByCustomerId(customerId);
        }

        return loans.stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> listInstallments(Long loanId) {
        Loan loan = loanRepository.findByIdWithInstallments(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        checkCustomerAccess(loan.getCustomer());

        List<LoanInstallment> installments = installmentRepository.findByLoanIdOrderByDueDateAsc(loanId);

        return installments.stream()
                .map((installment) -> {
                    int installmentNumber = installments.indexOf(installment) + 1;
                    return mapToInstallmentResponse(installment, installmentNumber);
                })
                .collect(Collectors.toList());
    }

    protected void checkCustomerAccess(Customer customer) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !customer.getUser().getId().equals(userDetails.getUser().getId())) {
            throw new AccessDeniedException("You don't have permission to access this customer's data");
        }
    }

    private LoanResponse mapToLoanResponse(Loan loan) {
        int paidInstallments = installmentRepository.countPaidInstallmentsByLoanId(loan.getId());

        return LoanResponse.builder()
                .id(loan.getId())
                .customerId(loan.getCustomer().getId())
                .customerName(loan.getCustomer().getName() + " " + loan.getCustomer().getSurname())
                .loanAmount(loan.getLoanAmount())
                .numberOfInstallments(loan.getNumberOfInstallment())
                .interestRate(loan.getInterestRate())
                .createDate(loan.getCreateDate())
                .isPaid(loan.getIsPaid())
                .totalAmount(loan.getLoanAmount().multiply(BigDecimal.ONE.add(loan.getInterestRate())))
                .paidInstallments(paidInstallments)
                .remainingInstallments(loan.getNumberOfInstallment() - paidInstallments)
                .build();
    }

    private InstallmentResponse mapToInstallmentResponse(LoanInstallment installment, int installmentNumber) {
        return InstallmentResponse.builder()
                .id(installment.getId())
                .loanId(installment.getLoan().getId())
                .amount(installment.getAmount())
                .paidAmount(installment.getPaidAmount())
                .dueDate(installment.getDueDate())
                .paymentDate(installment.getPaymentDate())
                .isPaid(installment.getIsPaid())
                .installmentNumber(installmentNumber)
                .build();
    }
}