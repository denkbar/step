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
package step.datapool.gsheet;

import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolConfiguration;


public class GoogleSheetv4DataPoolConfiguration extends DataPoolConfiguration {
	
	DynamicValue<String> fileId = new DynamicValue<String>("");
	DynamicValue<String> serviceAccountKey = new DynamicValue<String>("");
	DynamicValue<String> tabName = new DynamicValue<String>("");

	public GoogleSheetv4DataPoolConfiguration() {
		super();
	}

	public DynamicValue<String> getFileId() {
		return fileId;
	}

	public void setFileId(DynamicValue<String> fileId) {
		this.fileId = fileId;
	}

	public DynamicValue<String> getServiceAccountKey() {
		return serviceAccountKey;
	}

	public void setServiceAccountKey(DynamicValue<String> serviceAccountKey) {
		this.serviceAccountKey = serviceAccountKey;
	}

	public DynamicValue<String> getTabName() {
		return tabName;
	}

	public void setTabName(DynamicValue<String> tabName) {
		this.tabName = tabName;
	}
	
}
