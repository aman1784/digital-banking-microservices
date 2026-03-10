package com.bank.accountservice.repository;

import com.bank.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwnerUsername(String ownerUsername);

    @Modifying
    @Query(value = """
                    UPDATE Account a
                    SET a.balance = a.balance - :amount
                    WHERE a.id = :accountId
                    AND a.ownerUsername = :username
                    AND a.balance >= :amount
                    AND a.status = AccountStatus.ACTIVE
                    """)
    int withdrawIfSufficient(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount, @Param("username") String username);

    @Modifying
    @Query("""
           UPDATE Account a
           SET a.balance = a.balance + :amount
           WHERE a.id = :accountId
           AND a.ownerUsername = :username
           AND a.status = AccountStatus.ACTIVE
    """)
    int deposit(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount, @Param("username") String username);


    Optional<Account> findByIdAndOwnerUsername(Long id, String ownerUsername);
}
