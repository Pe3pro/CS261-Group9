package com.example.demo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;

/** DAO ของตาราง USERS */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /** ใช้หาอาจารย์ทุกคนเพื่อส่งแจ้งเตือนเมื่อมีคำร้องรอพิจารณา */
    List<User> findByRoleAndActiveTrue(Role role);
}
