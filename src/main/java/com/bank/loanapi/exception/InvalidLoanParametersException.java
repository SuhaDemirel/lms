package com.bank.loanapi.exception;

public class InvalidLoanParametersException extends RuntimeException {
    public InvalidLoanParametersException(String message) {
        super(message);
    }
}