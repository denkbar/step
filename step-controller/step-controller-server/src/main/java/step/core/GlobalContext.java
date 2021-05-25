/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.core;

import step.core.Controller.ServiceRegistrationCallback;
import step.core.access.UserAccessor;
import ch.exense.commons.core.collections.CollectionFactory;
import step.core.entities.EntityManager;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.ControllerPluginManager;
import step.core.scheduler.ExecutionTaskAccessor;

public class GlobalContext extends AbstractExecutionEngineContext {
	
	private ControllerPluginManager pluginManager;
	 
	private CollectionFactory collectionFactory;
	
	private ExecutionAccessor executionAccessor;
	
	private ExecutionTaskAccessor scheduleAccessor;
	
	private UserAccessor userAccessor;
	
	private ServiceRegistrationCallback serviceRegistrationCallback;
	
	private EntityManager entityManager;
	
	public GlobalContext() {
		super();
	}

	public CollectionFactory getCollectionFactory() {
		return collectionFactory;
	}

	public void setCollectionFactory(CollectionFactory collectionFactory) {
		this.collectionFactory = collectionFactory;
	}

	public ExecutionAccessor getExecutionAccessor() {
		return executionAccessor;
	}

	public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		this.executionAccessor = executionAccessor;
	}

	public ExecutionTaskAccessor getScheduleAccessor() {
		return scheduleAccessor;
	}

	public void setScheduleAccessor(ExecutionTaskAccessor scheduleAccessor) {
		this.scheduleAccessor = scheduleAccessor;
	}

	public UserAccessor getUserAccessor() {
		return userAccessor;
	}

	public void setUserAccessor(UserAccessor userAccessor) {
		this.userAccessor = userAccessor;
	}

	public ControllerPluginManager getPluginManager() {
		return pluginManager;
	}

	public void setPluginManager(ControllerPluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}
	
	public ServiceRegistrationCallback getServiceRegistrationCallback() {
		return serviceRegistrationCallback;
	}

	public void setServiceRegistrationCallback(
			ServiceRegistrationCallback serviceRegistrationCallback) {
		this.serviceRegistrationCallback = serviceRegistrationCallback;
	}

	public Version getCurrentVersion() {
		return Version.getCurrentVersion();
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
	
}
