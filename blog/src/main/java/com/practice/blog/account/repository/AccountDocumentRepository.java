package com.practice.blog.account.repository;



import com.practice.blog.account.entity.AccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

// AccountDocument의 Repository
public interface AccountDocumentRepository extends MongoRepository<AccountDocument, String> {
}