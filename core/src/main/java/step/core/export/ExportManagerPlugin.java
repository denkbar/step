package step.core.export;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.resources.ResourceManager;

@Plugin
public class ExportManagerPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		ResourceManager resourceManager = context.get(ResourceManager.class);
		ExportTaskManager exportTaskManager = new ExportTaskManager(resourceManager);
		context.put(ExportTaskManager.class, exportTaskManager);
		
		context.getServiceRegistrationCallback().registerService(ExportServices.class);
		context.getServiceRegistrationCallback().registerService(ImportServices.class);
		
		super.executionControllerStart(context);
	}

}
