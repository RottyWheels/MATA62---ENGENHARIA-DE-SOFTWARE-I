package br.ufba.veridt;

import org.springframework.boot.SpringApplication;

public class TestVeridtApplication {

	public static void main(String[] args) {
		System.setProperty("spring.docker.compose.enabled", "false");
		SpringApplication.from(VeridtApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
