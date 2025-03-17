package com.yju.team2.seilomun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


// 스케줄 어노테이션 만들기위해 추가
@EnableScheduling
@SpringBootApplication
public class SeilomunApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeilomunApplication.class, args);
	}

}
