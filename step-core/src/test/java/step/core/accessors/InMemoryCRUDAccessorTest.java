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
package step.core.accessors;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Test;


public class InMemoryCRUDAccessorTest {

	@Test
	public void test() {
		InMemoryCRUDAccessor<AbstractIdentifiableObject> inMemoryCRUDAccessor = new InMemoryCRUDAccessor<>();
		AbstractIdentifiableObject entity = new AbstractIdentifiableObject();
		inMemoryCRUDAccessor.save(entity);
		AbstractIdentifiableObject actualEntity = inMemoryCRUDAccessor.get(entity.getId());
		assertEquals(entity, actualEntity);
		
		actualEntity = inMemoryCRUDAccessor.get(entity.getId().toString());
		assertEquals(entity, actualEntity);
		
		List<AbstractIdentifiableObject> range = inMemoryCRUDAccessor.getRange(0, 1);
		assertEquals(1, range.size());
		assertEquals(entity, range.get(0));
		
		range = inMemoryCRUDAccessor.getRange(10, 1);
		assertEquals(0, range.size());
		
		ArrayList<AbstractIdentifiableObject> all = new ArrayList<>();
		inMemoryCRUDAccessor.getAll().forEachRemaining(e->all.add(e));
		assertEquals(1, all.size());
		
		all.clear();
		inMemoryCRUDAccessor.remove(entity.getId());
		inMemoryCRUDAccessor.getAll().forEachRemaining(e->all.add(e));
		assertEquals(0, all.size());
		
		ArrayList<AbstractIdentifiableObject> entities = new ArrayList<AbstractIdentifiableObject>();
		entities.add(new AbstractIdentifiableObject());
		entities.add(new AbstractIdentifiableObject());
		inMemoryCRUDAccessor.save(entities);
		inMemoryCRUDAccessor.getAll().forEachRemaining(e->all.add(e));
		assertEquals(2, all.size());
		
		entity = new AbstractIdentifiableObject();
		entity.setId(null);
		inMemoryCRUDAccessor.save(entity);
		assertNotNull(entity.getId());
	}
	
	@Test
	public void testFindByAttributes() {
		InMemoryCRUDAccessor<AbstractIdentifiableObject> inMemoryCRUDAccessor = new InMemoryCRUDAccessor<>();
		
		AbstractOrganizableObject entity = new AbstractOrganizableObject();
		entity.addAttribute("att1", "val1");
		entity.addAttribute("att2", "val2");
		
		testFindByAttributes(inMemoryCRUDAccessor, entity, true);
	}

	@Test
	public void testFindByCustomFields() {
		InMemoryCRUDAccessor<AbstractIdentifiableObject> inMemoryCRUDAccessor = new InMemoryCRUDAccessor<>();
		
		AbstractIdentifiableObject entity = new AbstractIdentifiableObject();
		entity.addCustomField("att1", "val1");
		entity.addCustomField("att2", "val2");
		
		testFindByAttributes(inMemoryCRUDAccessor, entity, false);
	}

	private void testFindByAttributes(InMemoryCRUDAccessor<AbstractIdentifiableObject> inMemoryCRUDAccessor,
			AbstractIdentifiableObject entity, boolean findAttributes) {
		inMemoryCRUDAccessor.save(entity);
		
		HashMap<String, String> attributes = new HashMap<>();
		attributes.put("att1", "val1");
		AbstractIdentifiableObject actual = inMemoryCRUDAccessor.findByAttributes(attributes);
		assertEquals(entity, actual);
		
		attributes.clear();
		attributes.put("att1", "val2");
		actual = inMemoryCRUDAccessor.findByAttributes(attributes);
		assertEquals(null, actual);
		
		if(findAttributes) {
			actual = inMemoryCRUDAccessor.findByAttributes(attributes, "attributes");
		} else {
			actual = inMemoryCRUDAccessor.findByAttributes(attributes, "customFields");
		}
		assertEquals(null, actual);
		
		attributes.clear();
		attributes.put("att1", "val1");
		attributes.put("att2", "val2");
		actual = inMemoryCRUDAccessor.findByAttributes(attributes);
		assertEquals(entity, actual);
		
		actual = inMemoryCRUDAccessor.findByAttributes(null);
		assertEquals(entity, actual);
		
		AbstractOrganizableObject entity2 = new AbstractOrganizableObject();
		inMemoryCRUDAccessor.save(entity2);
		
		Spliterator<AbstractIdentifiableObject> findManyByAttributes = inMemoryCRUDAccessor.findManyByAttributes(null);
		assertEquals(2, StreamSupport.stream(findManyByAttributes, false).collect(Collectors.toList()).size());
		
		findManyByAttributes = inMemoryCRUDAccessor.findManyByAttributes(new HashMap<>());
		assertEquals(2, StreamSupport.stream(findManyByAttributes, false).collect(Collectors.toList()).size());
		
		findManyByAttributes = inMemoryCRUDAccessor.findManyByAttributes(attributes);
		assertEquals(1, StreamSupport.stream(findManyByAttributes, false).collect(Collectors.toList()).size());
		
		if(findAttributes) {
			findManyByAttributes = inMemoryCRUDAccessor.findManyByAttributes(null, "attributes");
		} else {
			findManyByAttributes = inMemoryCRUDAccessor.findManyByAttributes(null, "customFields");
		}
		assertEquals(2, StreamSupport.stream(findManyByAttributes, false).collect(Collectors.toList()).size());
	}

}
