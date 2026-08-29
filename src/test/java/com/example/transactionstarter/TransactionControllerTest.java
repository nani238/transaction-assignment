package com.example.transactionstarter;

import com.example.transactionstarter.transaction.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void testCreateTransaction_Success() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TX1001",
                "CUST01",
                new BigDecimal("150.50"),
                "USD",
                TransactionType.DEPOSIT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId", is("TX1001")))
                .andExpect(jsonPath("$.customerId", is("CUST01")))
                .andExpect(jsonPath("$.amount", is(150.50)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void testCreateTransaction_DuplicateId_ThrowsConflict() throws Exception {
        Transaction existing = new Transaction("TX1002", "CUST01", new BigDecimal("100.00"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        transactionRepository.save(existing);

        CreateTransactionRequest request = new CreateTransactionRequest(
                "TX1002",
                "CUST01",
                new BigDecimal("200.00"),
                "USD",
                TransactionType.WITHDRAWAL
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void testGetTransactionById_Success() throws Exception {
        Transaction transaction = new Transaction("TX1003", "CUST02", new BigDecimal("75.00"), "EUR", TransactionType.TRANSFER, TransactionStatus.PENDING);
        transactionRepository.save(transaction);

        mockMvc.perform(get("/api/transactions/TX1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("TX1003")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void testGetTransactionById_NotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/UNKNOWN_ID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testGetTransactionsByCustomerId_Success() throws Exception {
        Transaction tx1 = new Transaction("TX1004", "CUST_A", new BigDecimal("10.00"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        Transaction tx2 = new Transaction("TX1005", "CUST_A", new BigDecimal("20.00"), "USD", TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED);
        Transaction tx3 = new Transaction("TX1006", "CUST_B", new BigDecimal("30.00"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);

        transactionRepository.save(tx1);
        transactionRepository.save(tx2);
        transactionRepository.save(tx3);

        mockMvc.perform(get("/api/transactions?customerId=CUST_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testUpdateTransactionStatus_Success() throws Exception {
        Transaction transaction = new Transaction("TX1007", "CUST03", new BigDecimal("50.00"), "GBP", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        transactionRepository.save(transaction);

        UpdateStatusRequest updateRequest = new UpdateStatusRequest(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/TX1007/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void testUpdateTransactionStatus_FromCompleted_ThrowsBadRequest() throws Exception {
        Transaction transaction = new Transaction("TX1008", "CUST03", new BigDecimal("50.00"), "GBP", TransactionType.DEPOSIT, TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        UpdateStatusRequest updateRequest = new UpdateStatusRequest(TransactionStatus.FAILED);

        mockMvc.perform(patch("/api/transactions/TX1008/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}