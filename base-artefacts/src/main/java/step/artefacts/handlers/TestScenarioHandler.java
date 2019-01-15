/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.artefacts.handlers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import step.artefacts.TestScenario;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class TestScenarioHandler extends ArtefactHandler<TestScenario, ReportNode> {

	@Override
	public void createReportSkeleton_(ReportNode node, TestScenario testArtefact) {
		for(AbstractArtefact child:getChildren(testArtefact)) {
			delegateCreateReportSkeleton(child, node);
		}
	}

	@Override
	public void execute_(final ReportNode node, TestScenario testArtefact) {
		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(node.getStatus());
		
		List<AbstractArtefact> artefacts = getChildren(testArtefact);
		ExecutorService executor = Executors.newFixedThreadPool(artefacts.size());
		for(final AbstractArtefact child:artefacts) {
			executor.submit(new Runnable() {
				public void run() {
					context.associateThread();
					ReportNode childReportNode = delegateExecute(child, node);
					reportNodeStatusComposer.addStatusAndRecompose(childReportNode.getStatus());
				}
			});
		}
		
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			logger.error("An error occcurred while waiting for the executor to terminate",e);
		}
		
		node.setStatus(reportNodeStatusComposer.getParentStatus());
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestScenario testArtefact) {
		return new ReportNode();
	}

}
