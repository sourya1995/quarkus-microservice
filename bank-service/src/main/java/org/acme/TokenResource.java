package org.acme;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Authenticated
@Path("/token")
public class TokenResource {
	@Inject
	JsonWebToken accessToken;
	
	@GET
	@Path("/tokeninfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> token(){
		HashSet<String> set = new HashSet<String>();
		for(String t: accessToken.getClaimNames()) {
			set.add(t + "=" + accessToken.getClaim(t));
		}
		
		return set;
	}
}
