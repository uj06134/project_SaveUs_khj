package com.example.Ex02.controller;

import com.example.Ex02.service.ChallengeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChallengesController {

    private final ChallengeService challengeService;

    @GetMapping("/challenges")
    public String challenges(Model model, HttpSession session,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "tag", required = false) String tag,
                             @RequestParam(value = "page", defaultValue = "1") int page) {

        // 1. 사용자 ID 가져오기
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            userId = 0L; // 비로그인 사용자는 0L로 처리
        }

        // 2. "나의 챌린지" 탭 데이터 (기본 탭)
        model.addAttribute("summary", challengeService.getMyChallengeSummary(userId));
        model.addAttribute("ongoingChallenges", challengeService.findMyChallengesByStatus(userId, "ONGOING"));
        model.addAttribute("completedChallenges", challengeService.findMyChallengesByStatus(userId, "COMPLETED"));

        // 3. "둘러보기" 탭 데이터
        model.addAttribute("aiRecommendedChallenges", challengeService.findAiRecommendedChallenges(userId));
        model.addAttribute("allTags", challengeService.findAllTags());

        // 4. "둘러보기" - 페이징된 전체 챌린지 목록
        int size = 5; // 한 페이지에 5개씩
        Map<String, Object> challengePageData = challengeService.findAllChallengesPaginated(userId, keyword, tag, page, size);
        model.addAttribute("challengePage", challengePageData);
        model.addAttribute("currentKeyword", keyword); // 검색어 유지를 위해
        model.addAttribute("currentTag", tag); // 태그 활성화 유지를 위해

        // 5. "사이드바" 랭킹 데이터
        model.addAttribute("leaderboard", challengeService.findLeaderboardTop5());

        return "challenges";
    }
}