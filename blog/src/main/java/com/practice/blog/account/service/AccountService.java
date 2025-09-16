package com.practice.blog.account.service;

import com.practice.blog.account.dto.response.AccountResponseDto;
import com.practice.blog.account.dto.response.CreateAccountResponseDto;
import com.practice.blog.account.dto.request.BioUpdateRequestDto;
import com.practice.blog.account.dto.request.CreateAccountRequestDto;
import com.practice.blog.account.entity.Account;
import com.practice.blog.account.entity.AccountDocument;
import com.practice.blog.account.entity.AccountStatus;
import com.practice.blog.account.repository.AccountDocumentRepository;
import com.practice.blog.account.repository.AccountsRepository;
//import com.practice.blog.account.entity.AccountDocument;
//import com.practice.blog.account.repository.AccountDocumentRepository;

import com.practice.blog.global.exception.BlogException;
import com.practice.blog.global.exception.ExceptionCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
//import jakarta.annotation.PostConstruct;

//import org.springframework.data.redis.core.HashOperations;
//import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

//import java.util.Map;
//import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor

public class AccountService {

    private final AccountsRepository accountsRepository;
    private final AccountDocumentRepository accountDocumentRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, Object> hashOperations;
    private static final String ACCOUNT_CACHE_KEY = "Account";

    // 초기화
    @PostConstruct
    public void init() {
        this.hashOperations = redisTemplate.opsForHash();
    }


    // 회원 생성
    @Transactional
    public CreateAccountResponseDto createAccount(CreateAccountRequestDto requestDto) {
        if(accountsRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        Account account = requestDto.toEntity();
        Account savedAccount = accountsRepository.save(account);

        // Redis 해시에 이메일과 닉네임 저장
        String redisKey = ACCOUNT_CACHE_KEY + savedAccount.getAccountId();
        hashOperations.put(redisKey, "email", savedAccount.getEmail());
        hashOperations.put(redisKey, "nickname", savedAccount.getNickname());

        // 만료 시간 설정 (30분)
        redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);

        // MongoDB에 저장
        AccountDocument accountDocument = AccountDocument.builder()
                .id(savedAccount.getAccountId().toString())
                .email(savedAccount.getEmail())
                .nickname(savedAccount.getNickname())
                .password(savedAccount.getPassword())
                .build();
        accountDocumentRepository.save(accountDocument);
        return CreateAccountResponseDto.from(savedAccount);
    }


    // 회원 수정 (bio, nickname)
    @Transactional
    public AccountResponseDto updateAccount(Long accountId, BioUpdateRequestDto requestDto) {
        Account account = findByAccountId(accountId);
        account.updateBio(requestDto.getBio());
        account.updateNickname(requestDto.getNickname());

        // Redis에서 닉네임 업데이트 (bio는 Redis에 저장하지 않으므로 생략)
        String redisKey = ACCOUNT_CACHE_KEY + accountId;
        hashOperations.put(redisKey, "nickname", account.getNickname());

        // MonboDB에서 닉네임 업데이트
        String _id = accountId.toString();
        AccountDocument accountDocument = accountDocumentRepository.findById(_id)
                .orElseThrow(() -> new BlogException(ExceptionCode.ACCOUNT_NOT_FOUND));

        accountDocument.update(account.getNickname());
        accountDocumentRepository.save(accountDocument);

        return AccountResponseDto.from(account);
    }


    // 회원 물리적 삭제
    @Transactional
    public void physicalDeleteAccount(Long accountId) {
        Account account = findByAccountId(accountId);

        // Redis에서 삭제
        String redisKey = ACCOUNT_CACHE_KEY + accountId;
        redisTemplate.delete(redisKey);

        // MySQL에서 삭제
        accountsRepository.delete(account);

        // MongoDB에서 삭제
        String _id = accountId.toString();
        if (!accountDocumentRepository.existsById(_id)) {
            throw new BlogException(ExceptionCode.ACCOUNT_NOT_FOUND);
        }
        accountDocumentRepository.deleteById(_id);
    }

    // Redis에서 ID로 이메일 조회
    @Transactional(readOnly = true)
    public String findEmailByIdFromRedis(Long id) {
        String redisKey = ACCOUNT_CACHE_KEY + id;

        // Redis 해시에서 값 조회
        Map<String, Object> hashEntries = hashOperations.entries(redisKey);
        if (hashEntries.isEmpty()) { // Redis에 값이 없으면
            // DB에서 조회
            Account account = findByAccountId(id);

            // DB에서 조회한 정보를 Redis에 저장
            hashOperations.put(redisKey, "email", account.getEmail());
            hashOperations.put(redisKey, "nickname", account.getNickname());
            redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES); // 만료시간 설정

            return account.getEmail();
        }

        String email = (String) hashEntries.get("email");
        return email;
    }

    // MongoDB에서 ID로 닉네임 조회
    @Transactional(readOnly = true)
    public String findNicknamaByIdFromMongo(Long id) {
        String _id = id.toString();
        AccountDocument accountDocument = accountDocumentRepository.findById(_id)
                .orElseThrow(() -> new BlogException(ExceptionCode.ACCOUNT_NOT_FOUND));

        return accountDocument.getPassword();
    }

    // 회원 논리적 삭제 (status 변경)
    @Transactional
    public void deleteAccount(Long accountId) {
        Account account = findByAccountId(accountId);
        account.changeStatus(AccountStatus.DEACTIVATED);
    }

    // 회원 단건 조회
    @Transactional(readOnly=true)
    public AccountResponseDto getAccount(Long accountId) {
        Account account = findByAccountId(accountId);
        return AccountResponseDto.from(account);
    }

    @Transactional(readOnly=true)
    public Account findByAccountId(Long accountId) {
        return accountsRepository.findByAccountId(accountId)
                .orElseThrow(()-> new BlogException(ExceptionCode.ACCOUNT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Account findByEmail(String email){
        return accountsRepository.findByEmail(email)
                .orElseThrow(()-> new BlogException(ExceptionCode.ACCOUNT_NOT_FOUND));
    }

    // 이메일 중복 체크
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return accountsRepository.existsByEmail(email);
    }
}