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
package step.core.collections;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Id;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractIdentifiableObject;

public class Document extends DocumentObject {

	public Document() {
		super(new HashMap<>());
	}

	public Document(Map<String, Object> m) {
		super(m);
	}

	@Id
	public ObjectId getId() {
		return containsKey("_id") ? (ObjectId) get("_id") : new ObjectId((String) get(AbstractIdentifiableObject.ID));
	}

	@Override
	public String toString() {
		return super.toString();
	}
}