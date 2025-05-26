package com.bank.loanapi.repository;

import com.bank.loanapi.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerId(Long customerId);

    List<Loan> findByCustomerIdAndIsPaid(Long customerId, Boolean isPaid);

    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.installments WHERE l.id = :id")
    Optional<Loan> findByIdWithInstallments(@Param("id") Long id);

    @Query("SELECT l FROM Loan l WHERE l.customer.id = :customerId AND l.numberOfInstallment = :numberOfInstallments")
    List<Loan> findByCustomerIdAndNumberOfInstallments(@Param("customerId") Long customerId,
                                                       @Param("numberOfInstallments") Integer numberOfInstallments);
}