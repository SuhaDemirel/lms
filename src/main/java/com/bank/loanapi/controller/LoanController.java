package com.bank.loanapi.controller;

import com.bank.loanapi.dto.request.CreateLoanRequest;
import com.bank.loanapi.dto.request.PayLoanRequest;
import com.bank.loanapi.dto.response.InstallmentResponse;
import com.bank.loanapi.dto.response.LoanResponse;
import com.bank.loanapi.dto.response.PaymentResultResponse;
import com.bank.loanapi.service.LoanService;
import com.bank.loanapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Management", description = "Loan management APIs")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;
    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create Loan", description = "Create a new loan for a customer")
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        LoanResponse response = loanService.createLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List Loans", description = "List loans for a given customer with optional filters")
    public ResponseEntity<List<LoanResponse>> listLoans(
            @Parameter(description = "Customer ID") @RequestParam Long customerId,
            @Parameter(description = "Number of installments filter") @RequestParam(required = false) Integer numberOfInstallments,
            @Parameter(description = "Is paid filter") @RequestParam(required = false) Boolean isPaid) {
        List<LoanResponse> loans = loanService.listLoans(customerId, numberOfInstallments, isPaid);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{loanId}/installments")
    @Operation(summary = "List Installments", description = "List installments for a given loan")
    public ResponseEntity<List<InstallmentResponse>> listInstallments(
            @Parameter(description = "Loan ID") @PathVariable Long loanId) {
        List<InstallmentResponse> installments = loanService.listInstallments(loanId);
        return ResponseEntity.ok(installments);
    }

    @PostMapping("/{loanId}/pay")
    @Operation(summary = "Pay Loan", description = "Pay installments for a given loan")
    public ResponseEntity<PaymentResultResponse> payLoan(
            @Parameter(description = "Loan ID") @PathVariable Long loanId,
            @Valid @RequestBody PayLoanRequest request) {
        PaymentResultResponse response = paymentService.payLoan(loanId, request);
        return ResponseEntity.ok(response);
    }
}