package step.core.export;

import step.core.GlobalContext;
import step.core.imports.ImportServices;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.resources.ResourceManager;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies= {ResourceManagerControllerPlugin.class})
public class ExportManagerPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		ResourceManager resourceManager = context.getResourceManager();
		ExportTaskManager exportTaskManager = new ExportTaskManager(resourceManager);
		context.put(ExportTaskManager.class, exportTaskManager);
		
		context.getServiceRegistrationCallback().registerService(ExportServices.class);
		context.getServiceRegistrationCallback().registerService(ImportServices.class);
		
		super.executionControllerStart(context);
	}

}
