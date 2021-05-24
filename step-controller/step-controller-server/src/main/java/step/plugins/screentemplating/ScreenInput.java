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
package step.plugins.screentemplating;

import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;

public class ScreenInput extends AbstractOrganizableObject {

	protected String screenId;
	
	protected int position;
	
	protected Input input;

	public ScreenInput() {
		super();
	}

	public ScreenInput(int position, String screenId, Input input) {
		super();
		this.position = position;
		this.screenId = screenId;
		this.input = input;
	}
	
	public ScreenInput(String screenId, Input input) {
		super();
		this.screenId = screenId;
		this.input = input;
	}

	public String getScreenId() {
		return screenId;
	}

	public void setScreenId(String screenId) {
		this.screenId = screenId;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}
	
}
