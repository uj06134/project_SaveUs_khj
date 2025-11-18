package com.example.Ex02.service;

import com.example.Ex02.dto.*;
import com.example.Ex02.mapper.ChallengeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeMapper challengeMapper;

    @Override
    public MyChallengeSummaryDto getMyChallengeSummary(Long userId) {
        MyChallengeSummaryDto summary = new MyChallengeSummaryDto();
        if (userId == null || userId == 0) {
            summary.setActiveCount(0);
            summary.setCompletedCount(0);
            summary.setTotalPoints(0);
            return summary;
        }

        summary.setActiveCount(challengeMapper.countMyChallengesByStatus(userId, "ONGOING"));
        summary.setCompletedCount(challengeMapper.countMyChallengesByStatus(userId, "COMPLETED"));

        Integer points = challengeMapper.getMyTotalPoints(userId);
        summary.setTotalPoints(points != null ? points : 0);

        return summary;
    }

    @Override
    public List<MyChallengeItemDto> findMyChallengesByStatus(Long userId, String status) {
        if (userId == null || userId == 0) {
            return List.of(); // 비로그인 시 빈 리스트
        }
        return challengeMapper.findMyChallengesByStatus(userId, status);
    }

    @Override
    public List<ChallengeCardDto> findAiRecommendedChallenges(Long userId) {
        String mainGoal = null;
        if (userId != null && userId != 0) {
            mainGoal = challengeMapper.findUserMainGoal(userId);
        }

        // 사용자의 mainGoal이 없거나 비로그인 시, 기본값 (예: 'Weight Loss')
        if (mainGoal == null) {
            mainGoal = "Weight Loss"; // (임시 기본값)
        }

        return challengeMapper.findChallengesByGoal(mainGoal, userId);
    }

    @Override
    public List<TagDto> findAllTags() {
        return challengeMapper.findAllTags();
    }

    @Override
    public Map<String, Object> findAllChallengesPaginated(Long userId, String keyword, String tag, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("keyword", keyword);
        params.put("tag", tag);
        params.put("offset", (page - 1) * size);
        params.put("limit", size);

        List<ChallengeCardDto> challenges = challengeMapper.findAllChallengesPaginated(params);
        int totalCount = challengeMapper.countAllChallenges(params);

        Map<String, Object> result = new HashMap<>();
        result.put("challenges", challenges);
        result.put("totalCount", totalCount);
        result.put("currentPage", page);
        result.put("totalPages", (int) Math.ceil((double) totalCount / size));

        return result;
    }

    @Override
    public List<LeaderboardEntryDto> findLeaderboardTop5() {
        return challengeMapper.findLeaderboardTop5();
    }
}