package step.core.plans;

import java.util.Map;

public class LocalPlanRepository implements PlanRepository {

	protected PlanAccessor planAccessor;

	public LocalPlanRepository(PlanAccessor planAccessor) {
		super();
		this.planAccessor = planAccessor;
	}
	
	@Override
	public Plan load(Map<String, String> attributes) {
		Plan plan = planAccessor.findByAttributes(attributes);
		return plan;
	}
	
	@Override
	public void save(Plan plan) {
		planAccessor.save(plan);
	}
}
