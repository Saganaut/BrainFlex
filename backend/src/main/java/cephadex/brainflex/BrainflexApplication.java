package cephadex.brainflex;

import java.util.Date;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
// {@link cephadex.brainflex.service.NotificationEventListener} dispatches off
// the request thread so a STOMP push waiting on a broker reconnect doesn't
// stretch the originating HTTP call.
@EnableAsync
// Populates @CreatedDate / @LastModifiedDate on the Auditable base class so
// documents with createdAt + updatedAt don't need manual stamping in services.
@EnableMongoAuditing
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
