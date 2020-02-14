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

import step.artefacts.Sleep;
import step.common.managedoperations.OperationManager;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.functions.FunctionGroupHandle;

public class SleepHandler extends ArtefactHandler<Sleep, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Sleep testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Sleep testArtefact) {
		if (testArtefact.getReleaseTokens().get()) {
			releaseTokens(testArtefact);
		}
		long sleepDurationMs;
		try {
			sleepDurationMs = ((Number)testArtefact.getDuration().get()).longValue();
		} catch (NumberFormatException e) {
			throw new RuntimeException("Unable to parse attribute 'ms' as long.",e);
		}

		OperationManager.getInstance().enter("Sleep", sleepDurationMs);
		try {
			Thread.sleep(sleepDurationMs);
		} catch (InterruptedException e) {
		} finally {
			OperationManager.getInstance().exit();
		}
		
		node.setStatus(ReportNodeStatus.PASSED);		
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Sleep testArtefact) {
		return new ReportNode();
	}
}
