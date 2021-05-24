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
package step.core.objectenricher;

import step.core.AbstractContext;
import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.PojoFilter;
import ch.exense.commons.core.collections.PojoFilters.PojoFilterFactory;
import step.core.ql.OQLFilterBuilder;

public class ObjectPredicateFactory {

	private final ObjectHookRegistry objectHookRegistry;

	public ObjectPredicateFactory(ObjectHookRegistry objectHookRegistry) {
		super();
		this.objectHookRegistry = objectHookRegistry;
	}
	
	public ObjectPredicate getObjectPredicate(AbstractContext context) {
		ObjectFilter objectFilter = objectHookRegistry.getObjectFilter(context);
		String oqlFilter = objectFilter.getOQLFilter();
		Filter filter = OQLFilterBuilder.getFilter(oqlFilter);
		PojoFilter<Object> pojoFilter = new PojoFilterFactory<Object>().buildFilter(filter);
		return new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return pojoFilter.test(t);
			}
		};
	}
}
