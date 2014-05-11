package org.networkedassets.atlassian.stash.private_repositories_permissions.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.networkedassets.atlassian.stash.private_repositories_permissions.service.AllowedGroupsService;

@Path("/")
@Produces({ MediaType.APPLICATION_JSON })
public class GroupRestService {

	private final AllowedGroupsService allowedGroupsService;

	private final GroupInfoBuilder groupInfoBuilder;

	public GroupRestService(AllowedGroupsService allowedGroupsService,
			GroupInfoBuilder groupInfoBuilder) {
		this.allowedGroupsService = allowedGroupsService;
		this.groupInfoBuilder = groupInfoBuilder;
	}

	@Path("groups")
	@GET
	public List<UserInfo> getUsers() {
		return groupInfoBuilder.build();
	}

	@Path("group/${group}")
	@POST
	public Response addUser(@Context UriInfo uriInfo,
			@PathParam("group") String groupName) {
		this.allowedGroupsService.allow(groupName);
		return Response.created(uriInfo.getAbsolutePath()).build();
	}

	@Path("group/${group}")
	@DELETE
	public Response deleteUser(@PathParam("group") String groupName) {
		this.allowedGroupsService.disallow(groupName);
		return Response.ok().build();
	}

}
