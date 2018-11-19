package step.plugins.java.handler;

import javax.json.JsonObject;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.plugins.js223.handler.ScriptHandler;

public class GeneralScriptHandler extends JsonBasedFunctionHandler {

	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		pushRemoteApplicationContext(ScriptHandler.LIBRARIES_FILE, input.getProperties());
		
		String scriptLanguage = input.getProperties().get(ScriptHandler.SCRIPT_LANGUAGE);
		Class<?> handlerClass = scriptLanguage.equals("java")?JavaJarHandler.class:ScriptHandler.class;
		return delegate(handlerClass, input);		
	}

}
