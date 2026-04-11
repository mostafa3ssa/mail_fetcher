package com.emailorch.email_fetcher.repository;

import com.emailorch.email_fetcher.model.Transfer;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Repository
public interface TransferRepository extends JpaRepository<Transfer, @NonNull UUID> {

    @Query(value = "select email_sent_at from transfers order by email_sent_at DESC limit 1",nativeQuery = true)
    Instant findLatestTransfers();

}
