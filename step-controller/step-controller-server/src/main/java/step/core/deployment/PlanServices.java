package step.core.deployment;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.builder.PlanBuilder;

@Singleton
@Path("plans")
public class PlanServices extends AbstractServices {

protected PlanAccessor planAccessor;
	
	@PostConstruct
	public void init() {
		planAccessor = getContext().getPlanAccessor();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Plan newPlan(@QueryParam("type") String type) throws Exception {
		AbstractArtefact artefact = ArtefactRegistry.getInstance().getArtefactTypeInstance(type);
		Plan plan = PlanBuilder.create().startBlock(artefact).endBlock().build();
		return plan;
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Plan save(Plan plan) {
		return planAccessor.save(plan);
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Plan get(@PathParam("id") String id) {
		return planAccessor.get(id);
	}
	
	@GET
	@Path("/{id}/clone")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public Plan clonePlan(@PathParam("id") String id) {
		Plan plan = planAccessor.get(id);
		plan.setId(new ObjectId());
		plan.setRoot(cloneArtefact(plan.getRoot()));
		planAccessor.save(plan);
		return plan;
	}
	
	@POST
	@Path("/search")
	@Secured(right="plan-read")
	public Plan get(Map<String,String> attributes) {
		return planAccessor.findByAttributes(attributes);
	}
	
	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void delete(@PathParam("id") String id) {
		planAccessor.remove(new ObjectId(id));
	}
	
	@POST
	@Path("/artefacts/clone")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public AbstractArtefact cloneArtefact(AbstractArtefact artefact) {
		assignNewId(artefact);
		return artefact;
	}
	
	private void assignNewId(AbstractArtefact artefact) {
		artefact.setId(new ObjectId());
		artefact.getChildren().forEach(a->assignNewId(a));
	}
	
}
