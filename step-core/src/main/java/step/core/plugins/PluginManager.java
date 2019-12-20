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
package step.core.plugins;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager<T extends AbstractPlugin> implements InvocationHandler{
	
	private static Logger logger = LoggerFactory.getLogger(PluginManager.class);
	
	protected List<T> plugins = new CopyOnWriteArrayList<>();
	
	public void initialize() throws Exception {
		loadAnnotatedPlugins();
	}
	
	public <CALLBACK extends PluginCallbacks> CALLBACK getProxy(Class<CALLBACK> interfaceClass) {
		@SuppressWarnings("unchecked")
		CALLBACK proxy = (CALLBACK) Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass }, this);
		return proxy;
	}
	
	@SuppressWarnings("unchecked")
	private void loadAnnotatedPlugins() throws InstantiationException, IllegalAccessException, CircularDependencyException  {
		Set<Class<?>> pluginClasses = new Reflections("step").getTypesAnnotatedWith(Plugin.class);
		logger.debug("Found plugins classes: "+pluginClasses);
		
		for(Class<?> pluginClass:pluginClasses) {
			T plugin = newPluginInstance((Class<T>) pluginClass);
			if(plugin.validate())
				register(plugin);
		}
		
		plugins = sortPluginsByDependencies(plugins);
		logger.info("Loaded plugins in following order: "+plugins);
	}

	/**
	 * Sort the plugins according to their mutual dependencies.
	 * The plugin with the highest dependency to other plugins will be located at the end of the list.
	 * 
	 * @param plugins the unsorted list of plugins
	 * @return the sorted list of plugins
	 * @throws CircularDependencyException if a circular dependency is detected
	 */
	protected List<T> sortPluginsByDependencies(List<T> plugins) throws CircularDependencyException {
		
		
		// Create a list of additional dependencies based on the attribute "runsBefore"
		// The attribute "runsBefore" specifies a list of plugins before which a specific plugin should be executed.
		// Specifying that "A has to be run before B" has the same meaning as "B is depending on A"
		Map<Class<?>, List<Class<?>>> additionalDependencies = new HashMap<>();
		for (T plugin : plugins) {
			Class<? extends AbstractPlugin> pluginClass = plugin.getClass();
			Class<?>[] runsBeforeList = pluginClass.getAnnotation(Plugin.class).runsBefore();
			for (Class<?> runsBefore : runsBeforeList) {
				// 
				additionalDependencies.computeIfAbsent(runsBefore, c->new ArrayList<>()).add(pluginClass);
			}
		}
		
		List<T> result = new ArrayList<>(plugins);
		
		int iterationCount = 0;
		
		boolean hasModification = true;
		// loop as long as modifications to the ordering of the list are performed
		while(hasModification) {
			if(iterationCount>1000) {
				throw new CircularDependencyException("Circular dependency in the plugin dependencies");
			}
			
			hasModification = false;
			List<T> clone = new ArrayList<>(result);
			for (T plugin : result) {
				Class<? extends AbstractPlugin> pluginClass = plugin.getClass();
				Class<?>[] dependencies = pluginClass.getAnnotation(Plugin.class).dependencies();
				
				List<Class<?>> allDependencies = new ArrayList<>(Arrays.asList(dependencies));
				if(additionalDependencies.containsKey(pluginClass)) {
					allDependencies.addAll(additionalDependencies.get(pluginClass));
				}
				
				int initialPosition = clone.indexOf(plugin);
				int newPosition = -1;
				if(allDependencies.size()>0) {
					for (Class<?> dependency : allDependencies) {
						int positionOfDependencyInClone = IntStream.range(0, clone.size()).filter(i -> dependency.equals(clone.get(i).getClass())).findFirst().orElse(-1);
						// if the dependency is located after the current plugin  
						if(positionOfDependencyInClone>initialPosition) {
							// if this is the highest position of all dependencies of this plugin
							if(positionOfDependencyInClone>newPosition) {
								newPosition = positionOfDependencyInClone;
							}
						}
					}
				}
				if(newPosition>=0) {
					// move the plugin after the dependency with the highest position
					clone.add(newPosition+1, plugin);
					clone.remove(initialPosition);
					hasModification = true;
				}
			}
			
			result = clone;
			iterationCount++;
		}
		
		return result;
	}
	
	@SuppressWarnings("serial")
	public static class CircularDependencyException extends Exception {

		public CircularDependencyException(String message) {
			super(message);
		}
		
	}

	public void register(T plugin) {
		plugins.add(plugin);
	}

	@SuppressWarnings("unchecked")
	private T newPluginInstance(Class<T> _class) throws InstantiationException, IllegalAccessException  {
		AbstractPlugin plugin = _class.newInstance();
		return (T) plugin;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		for(AbstractPlugin plugin:plugins) {
			try {
				method.invoke(plugin, args);
			} catch (Throwable e) {
				logger.error("Error invoking method #" + method.getName() + " of plugin '" + plugin.getClass().getName() + "'" + "(" + e.toString() + ")", e);
			}
		}
		return null;
	}
}
