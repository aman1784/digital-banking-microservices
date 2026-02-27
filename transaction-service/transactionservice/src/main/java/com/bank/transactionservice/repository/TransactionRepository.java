package com.bank.transactionservice.repository;

import com.bank.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUsername(String username);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdAndUsername(@Param("accountId") Long accountId, @Param("username") String username);
}
