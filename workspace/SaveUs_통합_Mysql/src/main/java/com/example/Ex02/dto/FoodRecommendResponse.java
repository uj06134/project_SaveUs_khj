package com.example.Ex02.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FoodRecommendResponse {

    private int user_id;

    private Goal goal;
    private Current current;

    private List<String> deficit;
    private List<FoodItem> recommended;

    @Data
    public static class Goal {
        private double goal_calories;
        private double goal_protein;
        private double goal_carbs;
        private double goal_fat;
    }

    @Data
    public static class Current {
        private double calories;
        private double carbs;
        private double protein;
        private double fat;
        private double fiber;
        private double sodium;
    }

    @Data
    public static class FoodItem {
        private String food_name;
        private String category;
    }
}
