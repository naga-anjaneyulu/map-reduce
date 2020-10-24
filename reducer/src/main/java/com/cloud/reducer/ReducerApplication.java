package com.cloud.reducer;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReducerApplication {

	public static void main(String[] args) throws IOException {
		
		ApplicationContext ctx = SpringApplication.run(ReducerApplication.class, args);
		Reducer reducer = (Reducer) ctx.getBean("reduce");
		reducer.start();
	}
	
	

			
	
	

}
