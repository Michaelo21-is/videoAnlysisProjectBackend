package com.moj.userservice.Repository;

import com.moj.userservice.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    @Modifying
    @Transactional
    @Query("""
        UPDATE Users u
        SET u.creditSum = COALESCE(u.creditSum, 0) + :amount
        WHERE u.id = :userId
    """)
    int addCredit(
            @Param("userId") UUID userId,
            @Param("amount") Long amount
    );

}
