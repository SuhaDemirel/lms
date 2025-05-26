package com.bank.loanapi.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String message;
    private String error;
    private Integer status;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;
}
