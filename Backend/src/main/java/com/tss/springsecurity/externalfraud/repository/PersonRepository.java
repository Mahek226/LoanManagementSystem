package com.tss.springsecurity.externalfraud.repository;

import com.tss.springsecurity.externalfraud.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    
    Optional<Person> findByPanNumber(String panNumber);
    
    Optional<Person> findByAadhaarNumber(String aadhaarNumber);
    
    List<Person> findByPhoneNumber(String phoneNumber);
    
    List<Person> findByEmail(String email);
    
    @Query("SELECT p FROM Person p WHERE p.panNumber = :pan OR p.aadhaarNumber = :aadhaar")
    List<Person> findByPanOrAadhaar(@Param("pan") String panNumber, @Param("aadhaar") String aadhaarNumber);
    
    @Query("SELECT p FROM Person p WHERE p.firstName = :firstName AND p.lastName = :lastName AND p.dob = :dob")
    List<Person> findByNameAndDob(@Param("firstName") String firstName, 
                                  @Param("lastName") String lastName, 
                                  @Param("dob") java.time.LocalDate dob);
    
    @Query("SELECT COUNT(p) FROM Person p WHERE p.panNumber = :pan")
    long countByPanNumber(@Param("pan") String panNumber);
    
    @Query("SELECT COUNT(p) FROM Person p WHERE p.aadhaarNumber = :aadhaar")
    long countByAadhaarNumber(@Param("aadhaar") String aadhaarNumber);
    
    @Query("SELECT COUNT(p) FROM Person p WHERE p.phoneNumber = :phone")
    long countByPhoneNumber(@Param("phone") String phoneNumber);
    
    @Query("SELECT COUNT(p) FROM Person p WHERE p.email = :email")
    long countByEmail(@Param("email") String email);
}
