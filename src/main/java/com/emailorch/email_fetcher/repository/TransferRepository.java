package com.emailorch.email_fetcher.repository;

import com.emailorch.email_fetcher.model.Status;
import com.emailorch.email_fetcher.model.Transfer;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, @NonNull UUID> {

    Page<Transfer> findByUidOrderByEmailSentAtDesc(Long uid, Pageable pageable);

    long countByUid(Long uid);

    @Query("SELECT MAX(t.emailSentAt) FROM Transfer t WHERE t.uid = :uid")
    Instant findLatestTransfersByUid(@Param("uid") Long uid);

    // CHANGED BACK: use fname, because Google's attId is a shapeshifter!
    boolean existsByUidAndMsgIdAndFname(Long uid, String msgId, String fname);

    Optional<Transfer> findByUidAndMsgIdAndFname(Long uid, String msgId, String fname);
}