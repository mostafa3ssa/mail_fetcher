package com.emailorch.email_fetcher;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class EmailFetcherApplication {

	public static void main(String[] args) {
		// Capture the ApplicationContext
		ApplicationContext ctx = SpringApplication.run(EmailFetcherApplication.class, args);
	}

	// Using a CommandLineRunner bean ensures this runs right after the context is loaded
//	@Bean
//	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//		return args -> {
//			System.out.println("---------- LISTING ALL BEANS PROVIDED BY SPRING BOOT ----------");
//
//			String[] beanNames = ctx.getBeanDefinitionNames();
//			Arrays.sort(beanNames);
//
//			for (String beanName : beanNames) {
//				System.out.println(beanName);
//			}
//
//			System.out.println("---------- TOTAL BEAN COUNT: " + ctx.getBeanDefinitionCount() + " ----------");
//		};
//	}
}