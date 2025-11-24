package com.example.Ex02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@EnableScheduling //스케줄러 ( 챌린지 성공 체크 )
@SpringBootApplication
public class Ex01Application {

	public static void main(String[] args) {
		SpringApplication.run(Ex01Application.class, args);
	}


	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
