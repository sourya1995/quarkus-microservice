package org.acme.transactions;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WiremockAccountService implements QuarkusTestResourceLifecycleManager {
	
	private WireMockServer wireMockServer;

	@Override
	public Map<String, String> start() {
		wireMockServer = new WireMockServer();
		wireMockServer.start();
		
		stubFor(get(urlEqualTo("/accounts/121212/balance"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("435.76")));
		
		stubFor(post(urlEqualTo("/accounts/121212/transaction"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		
		return Collections.singletonMap("io.quarkus.transactions.AccountService/mp-rest/url", wireMockServer.baseUrl());
	}

	@Override
	public void stop() {
		if( null != wireMockServer) {
			wireMockServer.stop();
		}
		
	}

}
