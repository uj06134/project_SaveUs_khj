package com.example.Ex02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication //메인있는곳에 반드시 써야함
public class Ex01Application {

	public static void main(String[] args) {

		SpringApplication.run(Ex01Application.class, args);
		System.out.println("시작");
	}

}
