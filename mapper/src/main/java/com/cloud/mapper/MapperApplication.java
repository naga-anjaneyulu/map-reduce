package com.cloud.mapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan
public class MapperApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(MapperApplication.class, args);
		Mapper mapper = (Mapper) ctx.getBean("mapper");
		mapper.start();
	}

}
