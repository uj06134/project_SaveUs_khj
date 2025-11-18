package com.example.Ex02.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.Ex02.dto.*; // DTO 패키지 임포트
import java.util.List;
import java.util.Map;

@Mapper
public interface ChallengeMapper {

    List<ChallengeCardDto> findChallengesByGoal(@Param("goal") String goal, @Param("userId") Long userId);
    List<ChallengeCardDto> findChallengesByPersona(@Param("persona") String persona, @Param("userId") Long userId);
    List<TagDto> findAllTags();
    List<LeaderboardEntryDto> findLeaderboardTop5();
    List<ChallengeCardDto> findAllChallengesPaginated(Map<String, Object> params);
    int countAllChallenges(Map<String, Object> params);

    // 1. "나의 챌린지" - 요약 카드
    Integer countMyChallengesByStatus(@Param("userId") Long userId, @Param("status") String status);
    // 2. "나의 챌린지" - 요약 카드 (USERS 테이블에 TOTAL_POINTS가 있다고 가정) (TOTAL_POINTS를 추가?)
    Integer getMyTotalPoints(@Param("userId") Long userId);
    // 3. "나의 챌린지" - 진행중/완료 목록
    List<MyChallengeItemDto> findMyChallengesByStatus(@Param("userId") Long userId, @Param("status") String status);
    // 4. "둘러보기" - AI 추천 (사용자의 MAIN_GOAL 조회)
    String findUserMainGoal(@Param("userId") Long userId);
}