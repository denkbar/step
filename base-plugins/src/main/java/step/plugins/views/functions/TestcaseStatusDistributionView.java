package step.plugins.views.functions;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.TestCase;
import step.artefacts.reports.CallFunctionReportNode;
import step.artefacts.reports.TestCaseReportNode;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.plugins.views.AbstractView;
import step.plugins.views.View;

@View
public class TestcaseStatusDistributionView extends AbstractView<ReportNodeStatusDistribution> {	

	@Override
	public void afterReportNodeSkeletonCreation(ReportNodeStatusDistribution model, ReportNode node) {
		if(node instanceof TestCaseReportNode) {
			model.countForecast++;
		}
	}

	@Override
	public void afterReportNodeExecution(ReportNodeStatusDistribution model, ReportNode node) {
		if(node instanceof TestCaseReportNode) {
			model.distribution.get(node.getStatus()).count++;
			model.count++;
			if(model.countForecast<model.count) {
				model.countForecast=model.count;
			}
		}
	}

	@Override
	public ReportNodeStatusDistribution init() {
		Map<ReportNodeStatus, ReportNodeStatusDistribution.Entry> progress = new HashMap<>();
		for(ReportNodeStatus status:ReportNodeStatus.values()) {
			progress.put(status, new ReportNodeStatusDistribution.Entry(status));
		}
		return new ReportNodeStatusDistribution(progress);
	}

	@Override
	public String getViewId() {
		return "statusDistributionForTestcases";
	}
}