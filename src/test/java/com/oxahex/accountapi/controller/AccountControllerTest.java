package com.oxahex.accountapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxahex.accountapi.dto.AccountDto;
import com.oxahex.accountapi.dto.CreateAccount;
import com.oxahex.accountapi.dto.DeleteAccount;
import com.oxahex.accountapi.service.AccountService;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("계좌 생성 - 성공")
    void createAccount() throws Exception {
        // given: 계좌 생성 완료
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then: userId, accountNumber, registeredAt
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateAccount.Request(1L, 100L)
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
// TODO               .andExpect(jsonPath("$.registeredAt").value(LocalDateTime.now()))             시간값은?
                .andDo(print());
    }

    @Test
    @DisplayName("계좌 삭제 - 성공")
    void deleteAccount() throws Exception {
        // given: 계좌 삭제 완료
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then: userId, accountNumber, unRegisteredAt
        mockMvc.perform(delete("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new DeleteAccount.Request(333L, "9876543210")
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
// TODO               .andExpect(jsonPath("$.unRegisteredAt").value())
                .andDo(print());
    }

    @Test
    @DisplayName("계좌 조회 - 성공")
    void getAccountsByUserId() throws Exception {
        // given: 계좌 3개 존재
        List<AccountDto> accountDtos = Arrays.asList(
                AccountDto.builder().accountNumber("1234567890").balance(10000L).build(),
                AccountDto.builder().accountNumber("1234567891").balance(20000L).build(),
                AccountDto.builder().accountNumber("1234567892").balance(30000L).build()
        );
        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(accountDtos);

        // when
        // then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$[1].accountNumber").value("1234567891"))
                .andExpect(jsonPath("$[2].accountNumber").value("1234567892"))
                .andExpect(jsonPath("$[0].balance").value(10000L))
                .andExpect(jsonPath("$[1].balance").value(20000L))
                .andExpect(jsonPath("$[2].balance").value(30000L));
    }
}