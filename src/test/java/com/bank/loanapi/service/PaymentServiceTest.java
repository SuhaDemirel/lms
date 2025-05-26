package com.bank.loanapi.service;

import com.bank.loanapi.dto.request.PayLoanRequest;
import com.bank.loanapi.dto.response.PaymentResultResponse;
import com.bank.loanapi.entity.*;
import com.bank.loanapi.exception.ResourceNotFoundException;
import com.bank.loanapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanService loanService;

    @InjectMocks
    private PaymentService paymentService;

    private Loan testLoan;
    private Customer testCustomer;
    private List<LoanInstallment> testInstallments;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000"))
                .usedCreditLimit(new BigDecimal("1200"))
                .build();

        testLoan = Loan.builder()
                .id(1L)
                .customer(testCustomer)
                .loanAmount(new BigDecimal("1000"))
                .numberOfInstallment(6)
                .interestRate(new BigDecimal("0.2"))
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .installments(new ArrayList<>())
                .build();

        // Create 6 installments
        testInstallments = new ArrayList<>();
        LocalDate baseDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        for (int i = 0; i < 6; i++) {
            LoanInstallment installment = LoanInstallment.builder()
                    .id((long) (i + 1))
                    .loan(testLoan)
                    .amount(new BigDecimal("200"))
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(baseDate.plusMonths(i))
                    .isPaid(false)
                    .build();
            testInstallments.add(installment);
            testLoan.getInstallments().add(installment);
        }
    }

    @Test
    void payLoan_SingleInstallment_Success() {
        PayLoanRequest request = new PayLoanRequest(new BigDecimal("200"));

        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.of(testLoan));
        when(installmentRepository.findPayableInstallments(eq(1L), any(LocalDate.class)))
                .thenReturn(testInstallments.subList(0, 3)); // First 3 installments

        PaymentResultResponse response = paymentService.payLoan(1L, request);

        assertNotNull(response);
        assertEquals(1, response.getInstallmentsPaid());
        assertTrue(response.getTotalAmountSpent().compareTo(BigDecimal.ZERO) > 0);
        assertFalse(response.getIsLoanFullyPaid());

        verify(loanRepository).save(testLoan);
    }

    @Test
    void payLoan_MultipleInstallments_Success() {
        PayLoanRequest request = new PayLoanRequest(new BigDecimal("600"));

        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.of(testLoan));
        when(installmentRepository.findPayableInstallments(eq(1L), any(LocalDate.class)))
                .thenReturn(testInstallments.subList(0, 3));

        PaymentResultResponse response = paymentService.payLoan(1L, request);

        assertEquals(3, response.getInstallmentsPaid());
        assertFalse(response.getIsLoanFullyPaid());
    }

    @Test
    void payLoan_WithEarlyPaymentDiscount_Success() {
        PayLoanRequest request = new PayLoanRequest(new BigDecimal("200"));

        // Set due date to future for early payment
        testInstallments.get(0).setDueDate(LocalDate.now().plusDays(10));

        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.of(testLoan));
        when(installmentRepository.findPayableInstallments(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(testInstallments.get(0)));

        PaymentResultResponse response = paymentService.payLoan(1L, request);

        assertEquals(1, response.getInstallmentsPaid());
        // Should be less than 200 due to discount
        assertTrue(response.getTotalAmountSpent().compareTo(new BigDecimal("200")) < 0);
        assertEquals("EARLY", response.getPaidInstallments().get(0).getPaymentType());
    }

    @Test
    void payLoan_WithLatePaymentPenalty_Success() {
        PayLoanRequest request = new PayLoanRequest(new BigDecimal("210"));

        // Set due date to past for late payment
        testInstallments.get(0).setDueDate(LocalDate.now().minusDays(10));

        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.of(testLoan));
        when(installmentRepository.findPayableInstallments(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(testInstallments.get(0)));

        PaymentResultResponse response = paymentService.payLoan(1L, request);

        assertEquals(1, response.getInstallmentsPaid());
        // Should be more than 200 due to penalty
        assertTrue(response.getTotalAmountSpent().compareTo(new BigDecimal("200")) > 0);
        assertEquals("LATE", response.getPaidInstallments().get(0).getPaymentType());
    }

    @Test
    void payLoan_LoanNotFound_ThrowsException() {
        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.payLoan(1L, new PayLoanRequest(new BigDecimal("200"))));
    }

    @Test
    void payLoan_AlreadyPaid_ThrowsException() {
        testLoan.setIsPaid(true);
        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.of(testLoan));

        assertThrows(IllegalStateException.class,
                () -> paymentService.payLoan(1L, new PayLoanRequest(new BigDecimal("200"))));
    }

    @Test
    void payLoan_NoPayableInstallments_ThrowsException() {
        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.of(testLoan));
        when(installmentRepository.findPayableInstallments(eq(1L), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        assertThrows(IllegalStateException.class,
                () -> paymentService.payLoan(1L, new PayLoanRequest(new BigDecimal("200"))));
    }

    @Test
    void payLoan_FullPayment_UpdatesLoanStatus() {
        // Set only 2 unpaid installments
        testInstallments.subList(2, 6).forEach(i -> i.setIsPaid(true));
        PayLoanRequest request = new PayLoanRequest(new BigDecimal("400"));

        when(loanRepository.findByIdWithInstallments(1L)).thenReturn(Optional.of(testLoan));
        when(installmentRepository.findPayableInstallments(eq(1L), any(LocalDate.class)))
                .thenReturn(testInstallments.subList(0, 2));

        PaymentResultResponse response = paymentService.payLoan(1L, request);

        assertEquals(2, response.getInstallmentsPaid());
        assertTrue(response.getIsLoanFullyPaid());
        assertTrue(testLoan.getIsPaid());

        verify(customerRepository).save(testCustomer);
    }
}
