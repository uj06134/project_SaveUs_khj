package com.example.Ex02.service;

import com.example.Ex02.dto.*;
import com.example.Ex02.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MealMapper mealMapper;
    private final HealthScoreMapper healthScoreMapper;
    private final DailyIntakeMapper dailyIntakeMapper;
    private final UserGoalMapper userGoalMapper;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;

    public ReportDto getReportData(Long userId, String dateStr) {
        ReportDto report = new ReportDto();

        // 데이터 존재 여부 (해당 날짜의 영양소 기록 확인)
        MealDto dailyNutrition = mealMapper.findNutritionByDate(userId, dateStr);
        boolean hasRecord = (dailyNutrition != null && dailyNutrition.getCalories() > 0);
        report.setHasData(hasRecord);

        // -------------------------------------------------
        // 날짜와 상관없이 보여줄 통계 (건강점수, bmi, Top5)
        // -------------------------------------------------

        //유저 키 가져오기 (BMI 계산용) - 과거 bmi를 계산하기 위해 따로계산
        UserJoinDto user = userMapper.findById(userId);
        double heightM = (user.getHeight() != null ? user.getHeight() : 170) / 100.0; // 미터 단위 변환


        // 건강 점수 추이 (최근 12일)
        List<CalendarScoreDto> scores = healthScoreMapper.selectRecentScores(userId);
        List<String> sDates = new ArrayList<>();
        List<Integer> sValues = new ArrayList<>();
        List<Double> bmiValues = new ArrayList<>();
        List<Double> weightValues = new ArrayList<>();

        int sumScore = 0;
        for (CalendarScoreDto s : scores) {
            sDates.add(s.getScoreDate().format(DateTimeFormatter.ofPattern("MM/dd")));
            sValues.add(s.getScore());
            sumScore += s.getScore();

            // 체중 및 BMI 처리
            if (s.getWeight() != null && s.getWeight() > 0) {
                weightValues.add(s.getWeight());
                // BMI = 체중 / (키 * 키)
                double bmi = s.getWeight() / (heightM * heightM);
                bmiValues.add(Math.round(bmi * 10) / 10.0); // 소수점 1자리
            } else {
                // 기록이 없으면 0 또는 null 처리 (그래프 끊김 방지를 위해 직전 값 사용하거나 null)
                weightValues.add(null);
                bmiValues.add(null);
            }
        }
        report.setScoreDates(sDates);
        report.setScoreValues(sValues);
        report.setBmiValues(bmiValues);
        report.setWeightValues(weightValues);
        report.setAverageScore(scores.isEmpty() ? 0 : sumScore / scores.size());

        // 식단 유형 변화 (최근 30일)
        List<DailyIntakeDto> dailyList = dailyIntakeMapper.findDailyIntake(userId);

        List<String> dDates = new ArrayList<>();
        List<Integer> cCodes = new ArrayList<>();
        List<Integer> pCodes = new ArrayList<>();
        List<Integer> fCodes = new ArrayList<>();

        int startIdx = Math.max(0, dailyList.size() - 30);
        for (int i = startIdx; i < dailyList.size(); i++) {
            DailyIntakeDto day = dailyList.get(i);
            dDates.add(day.getEatDate().format(DateTimeFormatter.ofPattern("MM/dd")));

            // 총 칼로리 계산 (DB에 있으면 그거 쓰고, 없으면 계산)
            int totalKcal = (day.getCarbs() * 4) + (day.getProtein() * 4) + (day.getFats() * 9);

            // 비율 계산
            double cPct = calcNutrientPercent(day.getCarbs(), 4, totalKcal);
            double pPct = calcNutrientPercent(day.getProtein(), 4, totalKcal);
            double fPct = calcNutrientPercent(day.getFats(), 9, totalKcal);

            // 코드 저장
            cCodes.add(getCarbLevel(cPct));
            pCodes.add(getProteinLevel(pPct));
            fCodes.add(getFatLevel(fPct));
        }

        report.setDietDates(dDates);
        report.setCarbCodes(cCodes);
        report.setProteinCodes(pCodes);
        report.setFatCodes(fCodes);

        // 자주 먹은 음식 Top 5
        report.setTopMeals(mealMapper.selectTop5Meals(userId));


        // -------------------------------------------------
        // 선택된 날짜 기준 상세 데이터 (기록 없으면 스킵)
        // -------------------------------------------------
        if (!hasRecord) return report;

        // 레이더 차트 (목표 대비 %)
        UserGoalDto goal = userGoalMapper.findUserGoal(userId);
        if (goal != null) {
            int carbPct = calcPercent(dailyNutrition.getCarbs(), goal.getCarbsG());
            int protPct = calcPercent(dailyNutrition.getProtein(), goal.getProteinG());
            int fatPct  = calcPercent(dailyNutrition.getFat(), goal.getFatsG());
            int sugarPct = calcPercent(dailyNutrition.getSugar(), 50); // 권장 50g
            int sodiumPct = calcPercent(dailyNutrition.getSodium(), 2000); // 권장 2000mg (WHO 기준)

            report.setRadarMyIntake(Arrays.asList(carbPct, protPct, fatPct, sugarPct, sodiumPct));
            report.setRadarGoal(Arrays.asList(100, 100, 100, 100, 100));
        } else {
            report.setRadarMyIntake(Arrays.asList(0,0,0,0,0));
            report.setRadarGoal(Arrays.asList(100,100,100,100,100));
        }

        // 비만 위험도 (AI 예측)
        try {
            // 그 날짜의 체중 (없으면 최근값)
            Double pastWeight = healthScoreMapper.findWeightByDate(userId, dateStr);
            System.out.println("test");
            if(pastWeight == null) pastWeight = 70.0; // 기본값

            System.out.println("pastWeight: "+pastWeight);
            String url = "http://3.37.90.119:8001/predict-risk/" + userId;
            // [디버깅] 요청 보내는 URL 확인
            System.out.println("AI Server Request URL: " + url);

            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            // [디버깅] AI 서버 응답값 확인
            System.out.println("AI Server Response: " + result);

            if (result != null && result.get("risk_score") != null) {
                double prob = Double.parseDouble(result.get("risk_score").toString());
                System.out.println(prob);
                report.setObesityProbability(prob);
            }
        } catch (Exception e) {
            System.out.println("========== 비만도 분석 에러 발생 ==========");
            e.printStackTrace();
            report.setObesityProbability(0);
        }

        // 당뇨 예측 결과
        DiabetesScoreDto dScore = mealMapper.selectDiabetesScoreByDate(userId, dateStr);
        if (dScore != null) {
            System.out.println(dScore);
            report.setDiabetesScore(dScore.getScore());
//            report.setDiabetesSimilarity(dScore.getSimilarity());
            report.setDiabetesSimilarity(100-dScore.getScore());
            report.setDiabetesRiskLevel(dScore.getRiskLevel());

            System.out.println(dScore.getScore());
            System.out.println(dScore.getSimilarity());
            System.out.println(dScore.getRiskLevel());
            int score = dScore.getScore();

            if (score >= 90) {
                report.setDiabetesRiskLevel("EXCELLENT");
                report.setDiabetesComment("최고예요! 완벽한 식단입니다. 이대로만 유지하세요. 💎");
            } else if (score >= 70) {
                report.setDiabetesRiskLevel("GOOD");
                report.setDiabetesComment("좋아요! 건강한 식습관을 잘 지키고 계시네요. 🌿");
            } else if (score >= 50) {
                report.setDiabetesRiskLevel("NORMAL");
                report.setDiabetesComment("보통입니다. 탄수화물이나 당류를 조금만 더 신경 써보세요. 🟡");
            } else if (score >= 30) {
                report.setDiabetesRiskLevel("WARNING");
                report.setDiabetesComment("주의! 당뇨 위험 식단과 " + (100 - score) + "% 유사합니다. 관리가 필요해요. 🟠");
            } else {
                report.setDiabetesRiskLevel("DANGER");
                report.setDiabetesComment("위험합니다! 식단이 당뇨 위험 식단과" + (100 - score) + "% 일치합니다. 개선이 시급합니다! 🚨");
            }
        } else {
            report.setDiabetesRiskLevel("-");
            report.setDiabetesComment("분석 데이터가 없습니다.");
        }

        return report;
    }

    private int calcPercent(Integer actual, Integer target) {
        if (target == null || target == 0) return 0;
        if (actual == null) actual = 0;
        return (int) ((double) actual / target * 100);
    }

    //칼로리에서 영양소 비율 계산
    private double calcNutrientPercent(int grams, int multiplier, int totalKcal) {
        if (totalKcal == 0) return 0.0;
        return ((double) grams * multiplier / totalKcal) * 100.0;
    }
    // 1: Low, 2: Balanced, 3: High

    // 탄수화물 (목표: 40~65%)
    public int getCarbLevel(double carbP) {
        if (carbP < 40.0) return 1;       // 저탄수
        else if (carbP <= 65.0) return 2; // 균형
        else return 3;                    // 고탄수
    }
    // 단백질 (목표: 15~30%)
    public int getProteinLevel(double protP) {
        if (protP < 15.0) return 1;       // 저단백
        else if (protP <= 30.0) return 2; // 균형
        else return 3;                    // 고단백
    }
    // 지방 (목표: 20~35%)
    public int getFatLevel(double fatP) {
        if (fatP < 20.0) return 1;        // 저지방
        else if (fatP <= 35.0) return 2;  // 균형
        else return 3;                    // 고지방
    }


}