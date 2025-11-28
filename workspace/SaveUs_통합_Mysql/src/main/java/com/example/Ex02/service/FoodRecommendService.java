package com.example.Ex02.service;

import com.example.Ex02.dto.FoodRecommendRequest;
import com.example.Ex02.dto.FoodRecommendResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FoodRecommendService {

    private final RestTemplate restTemplate;

    public FoodRecommendService() {
        this.restTemplate = new RestTemplate();
    }

    public FoodRecommendResponse getFoodRecommend(FoodRecommendRequest request) {

        String url = "http://3.37.90.119:8004/food/recommend";

        try {
            return restTemplate.postForObject(
                    url,
                    request,
                    FoodRecommendResponse.class
            );
        } catch (Exception e) {
            System.out.println("FastAPI 요청 실패: " + e.getMessage());
            return null;
        }
    }
}
