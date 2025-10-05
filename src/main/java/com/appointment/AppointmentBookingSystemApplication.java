package com.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.appointment.repository")
public class AppointmentBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppointmentBookingSystemApplication.class, args);
	}

}
