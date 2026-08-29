package com.example.transactionstarter.transaction;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        if (transactionRepository.existsById(request.getTransactionId())) {
            throw new DuplicateTransactionException("Transaction with ID " + request.getTransactionId() + " already exists");
        }

        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency(),
                request.getTransactionType(),
                TransactionStatus.PENDING
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(savedTransaction);
    }

    public TransactionResponse getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));
        return TransactionResponse.fromEntity(transaction);
    }

    public List<TransactionResponse> getTransactionsByCustomerId(String customerId) {
        return transactionRepository.findByCustomerId(customerId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TransactionResponse updateTransactionStatus(String transactionId, UpdateStatusRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));

        TransactionStatus currentStatus = transaction.getStatus();
        TransactionStatus newStatus = request.getStatus();

        if (currentStatus == TransactionStatus.COMPLETED || currentStatus == TransactionStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Cannot change status from terminal state: " + currentStatus);
        }

        transaction.setStatus(newStatus);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(updatedTransaction);
    }
}