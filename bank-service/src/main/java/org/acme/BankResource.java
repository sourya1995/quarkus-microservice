package org.acme;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;


import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/bank")
public class BankResource {
	
	@ConfigProperty(name="username")
	String username;
	
	@ConfigProperty(name="password")
	String password;
	@ConfigProperty(name="bank.name", defaultValue="Bank of Default")
	String name;
	
	@GET
	@Path("/name")
	@Produces(MediaType.TEXT_PLAIN)
	public String getName() {
		return name;
	}
	
	@ConfigProperty(name="app.mobileBanking")
	Optional<Boolean> mobileBanking;
	@GET
	@Path("/mobilebanking")
	@Produces(MediaType.TEXT_PLAIN)
	public Boolean getMobileBanking() {
		return mobileBanking.orElse(false);
	}
	
	@ConfigProperties(prefix="bank-support")
	BankSupportConfig supportConfig;
	
	@ConfigProperties
    BankSupportConfig config;

    @Inject
    BankSupportConfigMapping configMapping;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/supportmapping")
    public Map<String, String> getSupportMapping() {
        HashMap<String,String> map = getSupport();

        map.put("business.email", configMapping.business().email());
        map.put("business.phone", configMapping.business().phone());

        return map;
    }

	
	@GET
	@Path("/support")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, String> getSupport() {
		HashMap<String, String> map = new HashMap<>();
		map.put("email", supportConfig.email);
		map.put("phone", supportConfig.getPhone());
		
		return map;
	}
	
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/secrets")
    public Map<String, String> getSecrets() {
        HashMap<String,String> map = new HashMap<>();

        map.put("username", username);
        map.put("password", password);
       

        return map;
    }
	
	
	
}
