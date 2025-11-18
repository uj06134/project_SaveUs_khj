package com.example.Ex02.dto;

import lombok.Data;

@Data
public class MyChallengeItemDto {
    private Long challengeId;
    private String title;
    private String description;
    private String imageUrl; // 챌린지 아이콘/이미지
    private int progressPercent; // User_Challenges.PROGRESS_PERCENT
}