package com.bank.loanapi.repository;

import com.bank.loanapi.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {

    List<LoanInstallment> findByLoanIdOrderByDueDateAsc(Long loanId);

    @Query("SELECT li FROM LoanInstallment li WHERE li.loan.id = :loanId AND li.isPaid = false ORDER BY li.dueDate ASC")
    List<LoanInstallment> findUnpaidInstallmentsByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT li FROM LoanInstallment li WHERE li.loan.id = :loanId AND li.dueDate <= :maxDueDate AND li.isPaid = false ORDER BY li.dueDate ASC")
    List<LoanInstallment> findPayableInstallments(@Param("loanId") Long loanId, @Param("maxDueDate") LocalDate maxDueDate);

    @Query("SELECT COUNT(li) FROM LoanInstallment li WHERE li.loan.id = :loanId AND li.isPaid = true")
    Integer countPaidInstallmentsByLoanId(@Param("loanId") Long loanId);
}