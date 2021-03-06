package step.functions.packages;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.WebPlugin;
import step.core.tables.AbstractTable;
import step.core.tables.Table;
import step.core.tables.TableRegistry;
import step.functions.manager.FunctionManager;
import step.functions.packages.handlers.JavaFunctionPackageHandler;
import step.functions.packages.handlers.RepositoryArtifactFunctionPackageHandler;
import step.functions.plugin.FunctionControllerPlugin;
import step.plugins.java.GeneralScriptFunctionControllerPlugin;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.InputType;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;
import step.resources.ResourceManager;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies= {ResourceManagerControllerPlugin.class, FunctionControllerPlugin.class, ScreenTemplatePlugin.class, GeneralScriptFunctionControllerPlugin.class})
public class FunctionPackagePlugin extends AbstractControllerPlugin {

	public static final String FUNCTION_TABLE_EXTENSIONS = "functionTableExtensions";
	private static final Logger logger = LoggerFactory.getLogger(FunctionPackagePlugin.class);
	private FunctionPackageManager packageManager;
	private FunctionManager functionManager;
	private FunctionPackageAccessor packageAccessor;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		FileResolver fileResolver = context.getFileResolver();
		ResourceManager resourceManager = context.getResourceManager();
		
		packageAccessor = new FunctionPackageAccessorImpl(
				context.getCollectionFactory().getCollection("functionPackage", FunctionPackage.class));
		
		Configuration configuration = context.getConfiguration();
		functionManager = context.get(FunctionManager.class);
		packageManager = new FunctionPackageManager(packageAccessor, functionManager, resourceManager, fileResolver, configuration);
		packageManager.registerFunctionPackageHandler(new JavaFunctionPackageHandler(fileResolver, configuration));
		packageManager.registerFunctionPackageHandler(new RepositoryArtifactFunctionPackageHandler(resourceManager, fileResolver, configuration));

		packageManager.start();
		
		context.put(FunctionPackageManager.class, packageManager);
		
		Table<FunctionPackage> collection = new AbstractTable<>(
				context.getCollectionFactory().getCollection("functionPackage", FunctionPackage.class), true);
		context.get(TableRegistry.class).register("functionPackage", collection);

		context.getServiceRegistrationCallback().registerService(FunctionPackageServices.class);
		
		context.getEntityManager().register(new FunctionPackageEntity(FunctionPackageEntity.entityName, packageAccessor, context));


		
		registerWebapp(context, "/functionpackages/");
	}
	


	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputsIfNecessary(context);
		
		Configuration configuration = context.getConfiguration();
		String embeddedPackageFolder = configuration.getProperty("plugins.FunctionPackagePlugin.embeddedpackages.folder");
		if(embeddedPackageFolder != null) {
			EmbeddedFunctionPackageImporter embeddedFunctionPackageImporter = new EmbeddedFunctionPackageImporter(functionManager, packageAccessor, packageManager);
			embeddedFunctionPackageImporter.importEmbeddedFunctionPackages(embeddedPackageFolder);
		}
	}

	protected void createScreenInputsIfNecessary(GlobalContext context) {
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> functionTableExtensions = screenInputAccessor.getScreenInputsByScreenId(FUNCTION_TABLE_EXTENSIONS);
		boolean inputExist = functionTableExtensions.stream().filter(i->i.getInput().getId().equals("customFields.functionPackageId")).findFirst().isPresent();
		if(!inputExist) {
			Input input = new Input(InputType.TEXT, "customFields.functionPackageId", "Package", "", null);
			input.setValueHtmlTemplate("<function-package-link id='stBean.customFields.functionPackageId' />");
			input.setSearchMapperService("rest/table/functionPackage/searchIdsBy/attributes.name");
			screenInputAccessor.save(new ScreenInput(FUNCTION_TABLE_EXTENSIONS, input));
		}
	}
	
	@Override
	public void executionControllerDestroy(GlobalContext context) {
		try {
			packageManager.close();
		} catch (IOException e) {
			logger.error("Error while closing package manager", e);
		}
	}

	@Override
	public WebPlugin getWebPlugin() {
		WebPlugin webPlugin = new WebPlugin();
		webPlugin.getAngularModules().add("functionPackages");
		webPlugin.getScripts().add("functionpackages/js/controllers/functionPackages.js");
		return webPlugin;
	}

}
