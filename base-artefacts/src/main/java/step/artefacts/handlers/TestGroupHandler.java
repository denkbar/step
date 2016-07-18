package step.artefacts.handlers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import step.artefacts.Sequence;
import step.artefacts.TestGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.TestArtefactResultHandler;

public class TestGroupHandler extends ArtefactHandler<TestGroup, ReportNode> {
	
	public void createReportSkeleton_(ReportNode node, TestGroup testArtefact) {		
	}

	@Override
	public void execute_(final ReportNode node, final TestGroup testArtefact) {		
		final int numberOfUsers = asInteger(testArtefact.getUsers());
		final int duration = asInteger(testArtefact.getDuration());
		final int numberOfIterations = asInteger(testArtefact.getIterations());
		final long rampup;
		if(testArtefact.getRampup()!=null) {
			rampup =asInteger( testArtefact.getRampup());
		} else {
			if(testArtefact.getPacing()!=null) {
				rampup = asInteger(testArtefact.getPacing());
			} else {
				rampup = 0;
			}
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
		try {
			for(int j=0;j<numberOfUsers;j++) {
				final int groupID = j;
				final long localStartOffset = asInteger(testArtefact.getStartOffset())+(long)((1.0*groupID)/numberOfUsers*rampup);
				executor.submit(new Runnable() {
					public void run() {
						ExecutionContext.setCurrentContext(context);
						
						ReportNode iterationReportNode = null;
						try {
							Thread.sleep(localStartOffset);
							for(int i=0;i<numberOfIterations;i++) {
								long startTime = System.currentTimeMillis();
								
								if(context.isInterrupted()) {
									break;
								}
								
								ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
								Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Group_"+groupID+"_Iteration_"+i);
								
								for(AbstractArtefact child:getChildren(testArtefact)) {
									iterationTestCase.addChild(child.getId());
								}

								iterationReportNode = delegateExecute(iterationTestCase, node);
								
								if(testArtefact.getPacing()!=null) {
									long endTime = System.currentTimeMillis();
									long duration = endTime-startTime;
									long pacingWait = asInteger(testArtefact.getPacing())-duration;
									if(pacingWait>0) {
										Thread.sleep(pacingWait);
									} else {
										// TODO: this is an application warning. Instead of being logged it should be shown to the end-user in a warning console
										logger.warn("Pacing of TestGroup " + testArtefact.getName() + " in test " + ExecutionContext.getCurrentContext().getExecutionId() + " exceeded. " +
												"The iteration lasted " + duration + "ms. The defined pacing was: " + testArtefact.getPacing() + "ms.");
									}
								}
							}
						} catch (Exception e) {
							if(iterationReportNode!=null) {
								TestArtefactResultHandler.failWithException(iterationReportNode, e);
							}
						}
					}
				});
			}
			
			executor.shutdown();

			executor.awaitTermination(duration, TimeUnit.SECONDS);
			node.setStatus(ReportNodeStatus.PASSED);
		} catch (InterruptedException e) {
			TestArtefactResultHandler.failWithException(node, e);
		} finally {
			executor.shutdownNow();
		}
		
		
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestGroup testArtefact) {
		return new ReportNode();
	}
	
	
	

}
