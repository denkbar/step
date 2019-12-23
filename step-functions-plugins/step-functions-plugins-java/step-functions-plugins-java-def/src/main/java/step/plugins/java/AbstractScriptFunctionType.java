package step.plugins.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;
import step.grid.filemanager.FileVersionId;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.java.handler.GeneralScriptHandler;
import step.plugins.js223.handler.ScriptHandler;

public abstract class AbstractScriptFunctionType<T extends GeneralScriptFunction> extends AbstractFunctionType<T> {

	protected FileVersionId daemonJar;
	
	protected Configuration configuration;
	
	public AbstractScriptFunctionType(Configuration configuration) {
		super();
		this.configuration = configuration;
	}
	
	@Override
	public void init() {
		super.init();
		daemonJar = registerResource(getClass().getClassLoader(), "java-plugin-handler.jar", false);
	}

	public Map<String, String> getHandlerProperties(T function) {
		Map<String, String> props = new HashMap<>();
		props.put(ScriptHandler.SCRIPT_LANGUAGE, function.getScriptLanguage().get());		
		registerFile(function.getLibrariesFile(), ScriptHandler.LIBRARIES_FILE, props);
		addPluginLibsIfRequired(function.getScriptLanguage().get(), props);
		registerFile(function.getScriptFile(), ScriptHandler.SCRIPT_FILE, props);
		registerFile(function.getErrorHandlerFile(), ScriptHandler.ERROR_HANDLER_FILE, props);
		
		if(configuration.getPropertyAsBoolean("plugins.java.validate.properties")) {
			props.put(KeywordExecutor.VALIDATE_PROPERTIES, Boolean.TRUE.toString());
		}
		
		return props;
	}

	protected void addPluginLibsIfRequired(String scriptLanguage, Map<String, String> props) {
		String property = configuration.getProperty("plugins."+scriptLanguage+".libs", null);
		if(property != null) {
			registerFile(new File(property), ScriptHandler.PLUGIN_LIBRARIES_FILE, props);
		}
	}
	
	@Override
	public String getHandlerChain(GeneralScriptFunction function) {
		return GeneralScriptHandler.class.getName();
	}

	@Override
	public FileVersionId getHandlerPackage(GeneralScriptFunction function) {
		return daemonJar;
	}
	
	public static final Map<String, String> fileExtensionMap = new ConcurrentHashMap<>();
	{
		fileExtensionMap.put("groovy", "groovy");
		fileExtensionMap.put("python", "py");
		fileExtensionMap.put("javascript", "js");
	}
	
	protected File getDefaultScriptFile(GeneralScriptFunction function) {
		String filename = getScriptFilename(function);
		String scriptDir = configuration.getProperty("keywords.script.scriptdir");
		File file = new File(scriptDir+"/"+filename);
		return file;
	}

	private String getScriptFilename(GeneralScriptFunction function) {
		TreeSet<String> sortedKeys = new TreeSet<>(function.getAttributes().keySet());
		StringBuilder filename = new StringBuilder();
		Iterator<String> it = sortedKeys.iterator();
		while(it.hasNext()) {
			filename.append(function.getAttributes().get(it.next()));
			if(it.hasNext()) {
				filename.append("_");				
			}
		}
		filename.append(".").append(fileExtensionMap.get(getScriptLanguage(function)));
		return filename.toString();
	}

	protected String getScriptLanguage(GeneralScriptFunction conf) {
		return conf.getScriptLanguage().get();
	}
	
	protected File setupScriptFile(GeneralScriptFunction function, String templateFilename) throws SetupFunctionException {
		File templateScript = new File(configuration.getProperty("controller.dir")+"/data/templates/"+templateFilename);
		try {
			return setupScriptFile(function,new FileInputStream(templateScript));
		} catch (FileNotFoundException e) {
			throw new SetupFunctionException("Unable to apply template. The file '"+templateScript.getAbsolutePath()+"' doesn't exist");
		}
	}
	
	protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream) throws SetupFunctionException {
		File scriptFile;
		
		String scriptFilename = function.getScriptFile().get();
		
		if (scriptFilename.startsWith(FileResolver.RESOURCE_PREFIX)) {
			return null;
		}
		
		if(scriptFilename==null || scriptFilename.trim().length()==0) {
			scriptFile = getDefaultScriptFile(function);
			function.getScriptFile().setValue(scriptFile.getAbsolutePath());
		} else {
			scriptFile = new File(scriptFilename);
		}

		if(!scriptFile.exists()) {
			File folder = scriptFile.getParentFile();
			if (!folder.exists()) {
				try {
					Files.createDirectory(folder.toPath());
				} catch (IOException e) {
					throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get(Function.NAME), e);
				}
			}
			try {
				scriptFile.createNewFile();
			} catch (IOException e) {
				throw new SetupFunctionException("Unable to create script folder '"+folder.getAbsolutePath()+"' for function '"+function.getAttributes().get(Function.NAME), e);
			}
			
			if(templateStream!=null) {
				applyTemplate(scriptFile, templateStream);				
			}
		}
		
		return scriptFile;
	}
	
	private void applyTemplate(File scriptFile, InputStream templateScript) throws SetupFunctionException {
		if(templateScript!=null) {
			try {
				Files.copy(templateScript, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new SetupFunctionException("Unable to copy template from stream to '"+scriptFile.getAbsolutePath()+"'", e);
			}				
		} else {
			throw new SetupFunctionException("Unable to apply template. The stream is null");
		}
	}
	
	public File getScriptFile(T function) {
		String scriptFilePath = function.getScriptFile().get();
		return fileResolver.resolve(scriptFilePath);
	}
}
