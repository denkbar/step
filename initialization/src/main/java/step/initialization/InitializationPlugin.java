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
package step.initialization;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jongo.MongoCollection;

import step.artefacts.CallFunction;
import step.artefacts.Check;
import step.artefacts.TestCase;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.accessors.MongoDBAccessorHelper;
import step.core.artefacts.ArtefactAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.plugins.adaptergrid.FunctionRepositoryImpl;
import step.plugins.functions.types.GeneralScriptFunction;
import step.plugins.selenium.SeleniumFunction;

@Plugin
public class InitializationPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MongoCollection controllerLogs = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "controllerlogs");
		
		long runCounts = controllerLogs.count();
		
		if(runCounts==0) {
			// First start
			setupUsers(context);
			setupDemo(context);
			//setupExecuteProcessFunction(context);
		}
		
		insertLogEntry(controllerLogs);
		
		super.executionControllerStart(context);
	}

	private void setupUsers(GlobalContext context) {
		User user = new User();
		user.setUsername("admin");
		user.setRole("default");
		user.setPassword(UserAccessor.encryptPwd("init"));
		context.getUserAccessor().save(user);
	}

	private void insertLogEntry(MongoCollection controllerLogs) {
		ControllerLog logEntry = new ControllerLog();
		logEntry.setStart(new Date());
		controllerLogs.insert(logEntry);
	}
	
//	private void setupExecuteProcessFunction(GlobalContext context) {		
//		Function executeProcessFunction = createFunction("ExecuteProcess", "class:step.handlers.processhandler.ProcessHandler");
//		
//		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
//		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
//		functionRepository.addFunction(executeProcessFunction);
//	}

	private void setupDemo(GlobalContext context) {
		MongoCollection functionCollection = MongoDBAccessorHelper.getCollection(context.getMongoClient(), "functions");				
		FunctionRepositoryImpl functionRepository = new FunctionRepositoryImpl(functionCollection);
				
		Function javaFunction = addScriptFunction(functionRepository, "Demo_Keyword_Java", "java", "../data/scripts/demo-java-keyword/target/classes");
		Function javascriptFunction = addScriptFunction(functionRepository, "Demo_Keyword_Javascript", "javascript", "../data/scripts/Demo_Keyword_Javascript.js");
		
		Function googleSearch = addSeleniumFunction(functionRepository, "Google_Search", "java", "../data/scripts/demo-selenium-keyword/target/classes" );
		Function googleSearchMock = addSeleniumFunction(functionRepository, "Google_Search_Mock", "javascript", "../data/scripts/Google_Search_Mock.js" );
		
	}
//
//	private void createDemoForEachPlan(ArtefactAccessor artefacts, String planName)  {
//		CallFunction call1 = createCallFunctionWithCheck(artefacts,"Javascript_HttpGet","{\"url\":\"[[dataPool.url]]\"}","output.getString(\"data\").contains(\"[[dataPool.check]]\")");
//		
//		ForEachBlock forEach = new ForEachBlock();
//		CSVDataPool conf = new CSVDataPool();
//		conf.setFile(new DynamicValue<String>("../data/testdata/demo.csv"));
//		forEach.setDataSource(conf);
//		forEach.setDataSourceType("csv");
//		forEach.addChild(call1.getId());
//		artefacts.save(forEach);
//
//		Map<String, String> tcAttributes = new HashMap<>();
//		TestCase testCase = new TestCase();
//		testCase.setRoot(true);
//		
//		tcAttributes.put("name", planName);
//		testCase.setAttributes(tcAttributes);
//		testCase.addChild(forEach.getId());
//		artefacts.save(testCase);
//	}
//	
//	private void createDemoPlan(ArtefactAccessor artefacts, String planName, String functionId, String args, String check) {
//		Map<String, String> tcAttributes = new HashMap<>();
//		TestCase testCase = new TestCase();
//		testCase.setRoot(true);
//		
//		tcAttributes.put("name", planName);
//		testCase.setAttributes(tcAttributes);
//		
//		CallFunction call1 = createCallFunctionByIdWithCheck(artefacts, functionId, args, check);
//		
//		testCase.addChild(call1.getId());
//		
//		testCase.setRoot(true);
//		artefacts.save(testCase);
//	}

	private CallFunction createCallFunctionByIdWithCheck(ArtefactAccessor artefacts, String functionId, String args,
			String check) {
		CallFunction call1 = createCallFunctionById(functionId, args);

		if(check!=null) {
			Check check1 = new Check();
			check1.setExpression(new DynamicValue<>(check, ""));
			artefacts.save(check1);
			call1.addChild(check1.getId());
		}
		
		artefacts.save(call1);
		return call1;
	}
	
	private CallFunction createCallFunctionById(String functionId, String args) {
		CallFunction call1 = new CallFunction();
		call1.setFunctionId(functionId);
		call1.setArgument(new DynamicValue<String>(args));
		return call1;
	}
	
	private void createSeleniumDemoPlan(ArtefactAccessor artefacts, String browser) {
		Map<String, String> tcAttributes = new HashMap<>();
		TestCase testCase = new TestCase();
		testCase.setRoot(true);
		
		tcAttributes.put("name", "Demo_Selenium_" + browser);
		testCase.setAttributes(tcAttributes);
		
		CallFunction call1 = new CallFunction();
		call1.setFunction("{\"name\":\"Selenium_Start"+ browser +"\"}");
		call1.setArgument(new DynamicValue<String>("{}"));
		artefacts.save(call1);
		
		CallFunction call2 = new CallFunction();
		call2.setFunction("{\"name\":\"Selenium_Navigate\"}");
		call2.setArgument(new DynamicValue<String>("{\"url\":\"http://denkbar.io\"}"));
		artefacts.save(call2);
		
		testCase.addChild(call1.getId());
		testCase.addChild(call2.getId());
		
		testCase.setRoot(true);
		artefacts.save(testCase);
	}
	

	private Function addScriptFunction(FunctionRepositoryImpl functionRepository, String name, String scriptLanguage, String scriptFile) {
		GeneralScriptFunction function = new GeneralScriptFunction();
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put("name", name);
		function.setAttributes(kwAttributes);
		function.getScriptLanguage().setValue(scriptLanguage);
		function.getScriptFile().setValue(scriptFile);
		functionRepository.addFunction(function);
		return function;
	}
	
	private Function addSeleniumFunction(FunctionRepositoryImpl functionRepository, String name, String scriptLanguage, String scriptFile) {
		SeleniumFunction function = new SeleniumFunction();
		Map<String, String> kwAttributes = new HashMap<>();
		kwAttributes.put("name", name);
		function.setAttributes(kwAttributes);
		function.getScriptLanguage().setValue(scriptLanguage);
		function.getScriptFile().setValue(scriptFile);
		function.setSeleniumVersion("2.x");
		functionRepository.addFunction(function);
		return function;
	}
}
