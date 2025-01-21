package com.aluracursos.LiterAlura;

import com.aluracursos.LiterAlura.principal.Principal;
import com.aluracursos.LiterAlura.service.ApiService;
import com.aluracursos.LiterAlura.service.ConvierteDatos;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@SpringBootApplication
public class LiterAluraApplication  implements CommandLineRunner {

	private final Principal principal;


	// Constructor que inyecta la clase Principal
	public LiterAluraApplication(@Lazy Principal principal) {
		this.principal = principal;
	}

	public static void main(String[] args) {
		SpringApplication.run(LiterAluraApplication.class, args);
	}


	@Override
	public void run(String... args) throws Exception {
		principal.mostrarMenu();
	}

	@Bean
	public ApiService apiService() {
		return new ApiService();
	}

	@Bean
	public ConvierteDatos conversor() {
		return new ConvierteDatos();
	}

	public Principal getPrincipal() {
		return principal;
	}
}
