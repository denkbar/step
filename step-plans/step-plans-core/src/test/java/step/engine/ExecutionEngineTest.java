package step.engine;

import org.junit.Test;

import step.artefacts.CheckArtefact;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineException;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;

public class ExecutionEngineTest {

	@Test
	public void test() throws ExecutionEngineException {
		ExecutionEngine executionEngine = new ExecutionEngine();
		
		CheckArtefact artefact = new CheckArtefact();
		artefact.setExecutionRunnable(c->System.out.println("Test"));
		Plan plan = PlanBuilder.create().startBlock(artefact).endBlock().build();
		executionEngine.execute(plan);
	}

}