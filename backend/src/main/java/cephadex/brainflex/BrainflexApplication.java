package cephadex.brainflex;

import java.util.Date;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class BrainflexApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrainflexApplication.class, args);
	}

	@PostConstruct
	public void init() {
		// Setting Spring Boot SetTimeZone
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		System.out.println("Spring boot application running in UTC timezone :" + new Date());
}
}
