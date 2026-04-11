package com.emailorch.email_fetcher.repository;

import com.emailorch.email_fetcher.model.Transfer;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, @NonNull UUID> {

    // 1. Pagination: Fetches only the requested page for a SPECIFIC user, sorted newest first
    Page<Transfer> findByUidOrderByEmailSentAtDesc(Long uid, Pageable pageable);

    // 2. Count: Checks if THIS specific user has any records yet
    long countByUid(Long uid);

    // 3. Last Sync: Finds the most recent email we fetched for THIS specific user
    // We replace your native query with a JPQL query that filters by UID
    @Query("SELECT MAX(t.emailSentAt) FROM Transfer t WHERE t.uid = :uid")
    Instant findLatestTransfersByUid(@Param("uid") Long uid);

    // Check by filename, immune to Google's changing tokens
    boolean existsByUidAndMsgIdAndFname(Long uid, String msgId, String fname);
}