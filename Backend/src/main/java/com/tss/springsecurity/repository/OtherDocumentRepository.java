package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.OtherDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtherDocumentRepository extends JpaRepository<OtherDocument, Long> {
    List<OtherDocument> findByApplicant_ApplicantId(Long applicantId);
}
