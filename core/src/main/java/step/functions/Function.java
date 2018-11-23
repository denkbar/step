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
package step.functions;

import java.util.Map;

import javax.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;

@JsonTypeInfo(use=Id.CLASS,property="type")
public class Function extends AbstractOrganizableObject {
	
	protected DynamicValue<Integer> callTimeout = new DynamicValue<>(180000);
	protected JsonObject schema;
	protected Map<String, String> tokenSelectionCriteria;
	
	public Map<String, String> getTokenSelectionCriteria() {
		return tokenSelectionCriteria;
	}

	public void setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
		this.tokenSelectionCriteria = tokenSelectionCriteria;
	}

	public static final String NAME = "name";
	
	public DynamicValue<Integer> getCallTimeout() {
		return callTimeout;
	}

	public void setCallTimeout(DynamicValue<Integer> callTimeout) {
		this.callTimeout = callTimeout;
	}
	
	public JsonObject getSchema() {
		return schema;
	}

	public void setSchema(JsonObject schema) {
		this.schema = schema;
	}
	
	public boolean requiresLocalExecution() {
		return false;
	}
}