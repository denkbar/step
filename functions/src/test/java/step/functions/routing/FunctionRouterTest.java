package step.functions.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.CallFunction;
import step.attachments.AttachmentManager;
import step.attachments.FileResolver;
import step.commons.conf.Configuration;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionTestHelper;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.tokenpool.Interest;

public class FunctionRouterTest {

	protected ExecutionContext context;
	
	@Before
	public void setupContext() {
		context = ExecutionTestHelper.setupContext();
	}
	
	@Test
	public void test() {
		
		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"callFunction\":\"cf\"}");
		
		Function function = new Function();
		Map<String, String> map = new HashMap<>();
		map.put("function", "f");
		function.setTokenSelectionCriteria(map);
		
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(new FileResolver(new AttachmentManager(new Configuration())), null);
		functionTypeRegistry.registerFunctionType(new AbstractFunctionType<Function>() {

			@Override
			public String getHandlerChain(Function function) {
				return null;
			}

			@Override
			public Map<String, String> getHandlerProperties(Function function) {
				return null;
			}

			@Override
			public Function newFunction() {
				return function;
			}

			@Override
			public Map<String, Interest> getTokenSelectionCriteria(Function function) {
				Map<String, Interest> map = new HashMap<>();
				map.put("functionType", new Interest(Pattern.compile("ft"), true));
				return map;
			}
		});
		
		
		FunctionExecutionService client = new FunctionExecutionServiceImpl(null, null, functionTypeRegistry, new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())));

		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		FunctionRouter router = new FunctionRouter(client, functionTypeRegistry, dynamicJsonObjectResolver);

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("route_to_key", "val");
		Map<String, Interest> selectionCriteria = router.buildSelectionCriteriaMap(callFunction, function, null, bindings);
		Assert.assertEquals("val", selectionCriteria.get("key").getSelectionPattern().pattern());
		Assert.assertEquals("ft", selectionCriteria.get("functionType").getSelectionPattern().pattern());
		Assert.assertEquals("cf", selectionCriteria.get("callFunction").getSelectionPattern().pattern());
		Assert.assertEquals("f", selectionCriteria.get("function").getSelectionPattern().pattern());

	}
}