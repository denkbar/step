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
package step.client.accessors;

import step.client.collections.remote.RemoteCollectionFactory;
import ch.exense.commons.core.accessors.AbstractAccessor;
import ch.exense.commons.core.model.accessors.AbstractIdentifiableObject;
import step.core.execution.model.ExecutionAccessor;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;
import step.parameter.ParameterAccessor;

public class RemoteAccessors {
	
	protected final RemoteCollectionFactory remoteCollectionFactory;

	public RemoteAccessors(RemoteCollectionFactory remoteCollectionFactory) {
		super();
		this.remoteCollectionFactory = remoteCollectionFactory;
	}

	public FunctionAccessor getFunctionAccessor() {
		return new RemoteFunctionAccessor(remoteCollectionFactory);
	}

	public PlanAccessor getPlanAccessor() {
		return new RemotePlanAccessor(remoteCollectionFactory);
	}
	
	public ExecutionAccessor getExecutionAccessor() {
		return new RemoteExecutionAccessor(remoteCollectionFactory);
	}

	public ParameterAccessor getParameterAccessor() {
		return new RemoteParameterAccessor(remoteCollectionFactory);
	}

	public <T extends AbstractIdentifiableObject> AbstractAccessor<T> getAbstractAccessor(String collectionId, Class<T> entityClass) {
		return new AbstractRemoteAccessor(remoteCollectionFactory,collectionId,entityClass);
	}
}
