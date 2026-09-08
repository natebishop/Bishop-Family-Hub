package com.familyhub.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// TODO: Configure a bounded ThreadPoolTaskExecutor bean for @Async (default SimpleAsyncTaskExecutor
//  creates unbounded threads). Also consider a custom AsyncUncaughtExceptionHandler to log/track
//  unexpected failures in async sync operations.
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class FamilyHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(FamilyHubApplication.class, args);
	}

}
