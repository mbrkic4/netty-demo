package com.example.demo;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.netty.http.client.HttpClient;

@SpringBootApplication
@RestController
public class DemoApplication {

	private static final Logger LOG = LoggerFactory.getLogger(DemoApplication.class);
	
	@Value("${server.port:8080}")
	int port;

	@GetMapping("/get")
	public ResponseEntity<String> get() {
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/redirect")
	public ResponseEntity<String> redirect() {
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create("http://localhost:" + port + "/get"));
		return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
	}

	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		
		for (int i=0; i<1000; i++) {
			test("/redirect");
		}
	}
	
	private void test(String uri) {
		State state = new State();
		
		HttpClient.create()
				.followRedirect(true)
				.doOnRequest((req, conn) -> state.value += "doOnRequest ")
				.doOnRedirect((res, conn) -> state.value += "doOnRedirect ")
				.get()
				.uri("http://localhost:" + port + uri)
				.responseContent() 
		        .aggregate()       
		        .asString()
				.block();
		
		if (!state.value.equals("doOnRequest doOnRedirect doOnRequest ")) {
			LOG.error("State: " + state.value);
			throw new RuntimeException(state.value);
		}
	}
	
	private static class State {
		String value = "";
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
