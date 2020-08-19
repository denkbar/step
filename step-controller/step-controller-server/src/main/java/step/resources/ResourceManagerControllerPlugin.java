package step.resources;

import java.io.File;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin()
public class ResourceManagerControllerPlugin extends AbstractControllerPlugin {

	protected ResourceAccessor resourceAccessor;
	protected ResourceRevisionAccessor resourceRevisionAccessor;
	protected ResourceManager resourceManager;
	protected FileResolver fileResolver;
	

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		resourceAccessor = new ResourceAccessorImpl(context.getMongoClientSession());
		resourceRevisionAccessor = new ResourceRevisionAccessorImpl(context.getMongoClientSession());
		String resourceRootDir = getResourceDir(context.getConfiguration());
		resourceManager = new ResourceManagerImpl(new File(resourceRootDir), resourceAccessor, resourceRevisionAccessor);
		context.put(ResourceAccessor.class, resourceAccessor);
		context.put(ResourceManager.class, resourceManager);
		context.getServiceRegistrationCallback().registerService(ResourceServices.class);
		
		fileResolver = new FileResolver(resourceManager);
		context.put(FileResolver.class, fileResolver);
		
		context.get(CollectionRegistry.class).register("resources", new Collection(context.getMongoClientSession().getMongoDatabase(), 
				"resources", Resource.class, true));
	}

	public static String getResourceDir(Configuration configuration) {
		String resourceRootDir = configuration.getProperty("resources.dir","resources");
		return resourceRootDir;
	}
}