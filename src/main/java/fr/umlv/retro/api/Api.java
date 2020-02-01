package fr.umlv.retro.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;


@Path("/api")
public class Api {

	@Inject
	EnvService service;

	@GET()
	@Path("capabilities")
	@Produces(MediaType.APPLICATION_JSON)
	public Capability capabilities() {
		return new Capability();
	}

	@GET()
	@Path("env/{envid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response env(@PathParam("envid") String envid) {
		return service.retrieve(envid);
	}

	@POST()
	@Path("env/")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response env(@MultipartForm MultipartFormDataInput input) throws Exception {
		return service.create(input);
	}

}