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
package step.core.execution;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;

import step.core.accessors.InMemoryCRUDAccessor;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.repositories.RepositoryObjectReference;

public class InMemoryExecutionAccessor extends InMemoryCRUDAccessor<Execution> implements ExecutionAccessor {

	@Override
	public void createIndexesIfNeeded(Long ttl) {
	}

	@Override
	public Execution get(String nodeId) {
		return get(new ObjectId(nodeId));
	}

	@Override
	public List<Execution> getActiveTests() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterable<Execution> findLastStarted(int limit) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Iterable<Execution> findLastEnded(int limit) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public List<Execution> getLastExecutionsBySchedulerTaskID(String schedulerTaskID, int limit) {
		return map.values().stream().filter(e->schedulerTaskID.equals(e.getExecutionTaskID())).sorted(new Comparator<Execution>() {
			@Override
			public int compare(Execution o1, Execution o2) {
				return -o1.getEndTime().compareTo(o2.getEndTime());
			}
		}).limit(limit).collect(Collectors.toList());
	}
}