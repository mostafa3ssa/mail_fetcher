package com.emailorch.email_fetcher.repository;

import com.emailorch.email_fetcher.model.Transfer;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface TransferRepository extends JpaRepository<Transfer, @NonNull UUID> {
}
