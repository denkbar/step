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
package step.artefacts;

import step.core.artefacts.Artefact;
import step.core.dynamicbeans.ContainsDynamicValues;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.datapool.DataPoolConfiguration;

@Artefact(name="DataSet")
public class DataSetArtefact extends AbstractForBlock {

	private DynamicValue<String> item = new DynamicValue<String>("dataSet");
	
	private DynamicValue<Boolean> resetAtEnd = new DynamicValue<Boolean>(false);
	
	private String dataSourceType;
	
	private DataPoolConfiguration dataSource;

	public DynamicValue<String> getItem() {
		return item;
	}

	public void setItem(DynamicValue<String> item) {
		this.item = item;
	}

	public String getDataSourceType() {
		return dataSourceType;
	}

	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	@ContainsDynamicValues
	@EntityReference(type= EntityManager.recursive)
	public DataPoolConfiguration getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataPoolConfiguration dataSource) {
		this.dataSource = dataSource;
	}
	public DynamicValue<Boolean> getResetAtEnd() {
		return resetAtEnd;
	}

	public void setResetAtEnd(DynamicValue<Boolean> resetAtEnd) {
		this.resetAtEnd = resetAtEnd;
	};
}
