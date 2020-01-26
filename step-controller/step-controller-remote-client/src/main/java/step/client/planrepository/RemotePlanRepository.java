package step.client.planrepository;

import java.util.Map;

import step.client.AbstractRemoteClient;
import step.client.accessors.RemotePlanAccessorImpl;
import step.client.credentials.ControllerCredentials;
import step.core.plans.Plan;
import step.core.plans.PlanRepository;

public class RemotePlanRepository extends AbstractRemoteClient implements PlanRepository {
	
	private RemotePlanAccessorImpl planAccessor;

	public RemotePlanRepository() {
		super();
		planAccessor = new RemotePlanAccessorImpl(credentials);
	}

	public RemotePlanRepository(ControllerCredentials credentials) {
		super(credentials);
		planAccessor = new RemotePlanAccessorImpl(credentials);
	}

	@Override
	public Plan load(Map<String, String> attributes) {
		return planAccessor.findByAttributes(attributes);
	}
	
	@Override
	public void save(Plan plan) {
		planAccessor.save(plan);
	}
}
