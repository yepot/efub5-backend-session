package com.practice.blog.account.controller;

import com.practice.blog.account.dto.response.AccountResponseDto;
import com.practice.blog.account.service.AccountService;
import com.practice.blog.account.dto.response.CreateAccountResponseDto;
import com.practice.blog.account.dto.request.BioUpdateRequestDto;
import com.practice.blog.account.dto.request.CreateAccountRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Redis에서 id로 이메일 조회
    @GetMapping("/redis/{accountId}")
    @ResponseStatus(value = HttpStatus.OK)
    public String getEmailByIdfromRedis(@PathVariable Long accountId) {
        return accountService.findEmailByIdFromRedis(accountId);
    }

    // Mongodb에서  id로 닉네임 조회
    @GetMapping("/mongodb/{accountId}")
    @ResponseStatus(value = HttpStatus.OK)
    public String getNicknameByIdFromMongodb(@PathVariable Long accountId) {
        return accountService.findNicknamaByIdFromMongo(accountId);
    }



    // 회원 조회: GET /accounts/{accountId}
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponseDto> getAccount(@PathVariable("accountId") Long accountId) {
        AccountResponseDto responseDto = accountService.getAccount(accountId);
        return ResponseEntity.ok(responseDto);
    }

    // 계정 생성 POST /accounts
    @PostMapping
    public ResponseEntity<CreateAccountResponseDto> createAccount(@RequestBody @Valid CreateAccountRequestDto requestDto) {
        CreateAccountResponseDto responseDto = accountService.createAccount(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // 계정 프로필(자기소개) 수정: PATCH /accounts/profile/{accountId}
    @PatchMapping("/profile/{accountId}")
    public ResponseEntity<AccountResponseDto> updateAccount(@PathVariable("accountId") Long accountId,
                                                            @RequestBody @Valid BioUpdateRequestDto requestDto) {
        AccountResponseDto responseDto = accountService.updateAccount(accountId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // 계정 논리적 삭제(탈퇴): PATCH /accounts/{accountId}
    @PatchMapping("/{accountId}")
    public ResponseEntity<String> deleteAccount(@PathVariable("accountId") Long accountId) {
        accountService.deleteAccount(accountId);  // 상태 변경만 수행
        return ResponseEntity.ok("message : 성공적으로 탈퇴되었습니다.");
    }

    // 계정 물리적 삭제: DELETE /accounts/{accountId}
    @DeleteMapping("/{accountId}")
    public ResponseEntity<String> physicalDeleteAccount(@PathVariable("accountId") Long accountId) {
        accountService.physicalDeleteAccount(accountId);
        return ResponseEntity.ok("message : 성공적으로 탈퇴되었습니다.");
    }
}
