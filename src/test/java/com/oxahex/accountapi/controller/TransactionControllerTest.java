package com.oxahex.accountapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxahex.accountapi.domain.Transaction;
import com.oxahex.accountapi.dto.AccountDto;
import com.oxahex.accountapi.dto.CancelBalance;
import com.oxahex.accountapi.dto.TransactionDto;
import com.oxahex.accountapi.dto.UseBalance;
import com.oxahex.accountapi.service.TransactionService;
import com.oxahex.accountapi.type.TransactionResultType;
import com.oxahex.accountapi.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.oxahex.accountapi.type.TransactionResultType.S;
import static com.oxahex.accountapi.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("잔액 사용 - 성공")
    void useBalance() throws Exception {
        // given: 잔액 사용 transaction 성공
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1234567890")
                        .transactedAt(LocalDateTime.now())
                        .amount(12345L)
                        .transactionId("transactionId")
                        .transactionResultType(TransactionResultType.S)
                        .build());

        // when
        // then
        mockMvc.perform(post("/transaction/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new UseBalance.Request(1L, "1234567890", 10000L)
                )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345));
    }

    @Test
    @DisplayName("잔액 취소 - 성공")
    void cancelBalance() throws Exception {
        // given: 잔액 취소 transaction 성공
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1234567890")
                        .transactedAt(LocalDateTime.now())
                        .amount(54321L)
                        .transactionId("transactionId")
                        .transactionResultType(TransactionResultType.S)
                        .build());

        // when
        // then
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelBalance.Request("transactionId", "1234567890", 10000L)
                        )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(54321));
    }

    @Test
    @DisplayName("거래 내역 조회 - 성공")
    void getQueryTransaction() throws Exception {
        // given: 거래

        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1234567890")
                        .transactionType(USE)
                        .transactedAt(LocalDateTime.now())
                        .amount(12345L)
                        .transactionId("transactionId")
                        .transactionResultType(S).build());

        // when
        // then
        mockMvc.perform(get("/transaction/12345"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345));
    }
}