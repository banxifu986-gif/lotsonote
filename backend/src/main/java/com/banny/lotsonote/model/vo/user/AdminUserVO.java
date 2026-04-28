package com.banny.lotsonote.model.vo.user;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long userId;

    private String account;

    private String username;

    private Integer gender;

    private LocalDate birthday;

    private String avatarUrl;

    private String email;

    private String school;

    private String signature;

    private Integer isBanned;

    private Integer isAdmin;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
