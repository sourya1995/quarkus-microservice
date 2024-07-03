package org.acme.transactions;

import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

import java.util.concurrent.TimeUnit;

public class FaultyAccountServiceTest {
	@Test
	void testTimeout() {
		given()
	      .contentType(ContentType.JSON)
	    .get("/transactions/123456/balance").then().statusCode(504);      // <2>

	    given()
	      .contentType(ContentType.JSON)
	    .get("/transactions/456789/balance").then().statusCode(200);
	}
	
	@Test
	  void testCircuitBreaker() {
	    RequestSpecification request =
	      given()
	        .body("142.12")
	        .contentType(ContentType.JSON);

	    request.post("/transactions/api/444666").then().statusCode(200);
	    request.post("/transactions/api/444666").then().statusCode(502);
	    request.post("/transactions/api/444666").then().statusCode(502);
	    request.post("/transactions/api/444666").then().statusCode(503);
	    request.post("/transactions/api/444666").then().statusCode(503);

	    try {
	      TimeUnit.MILLISECONDS.sleep(1000);
	    } catch (InterruptedException e) {
	    	Thread.currentThread().interrupt();
	    }

	    request.post("/transactions/api/444666").then().statusCode(200);
	    request.post("/transactions/api/444666").then().statusCode(200);
	  }
}
