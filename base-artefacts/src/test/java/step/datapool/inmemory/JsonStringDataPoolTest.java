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

package step.datapool.inmemory;

import org.junit.Test;

import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolFactory;
import step.datapool.DataSet;

public class JsonStringDataPoolTest{


	@Test
	public void testJsonDataPool() {
		

		JsonStringDataPoolConfiguration poolConf = new JsonStringDataPoolConfiguration();
		poolConf.setJson(new DynamicValue<String>("{ \"a\" : [\"va1\", \"va2\", \"va3\"], \"b\" : [\"vb1\", \"vb2\", \"vb3\"] }"));

		DataSet<?> pool = DataPoolFactory.getDataPool("json", poolConf, null);

		pool.init();
		pool.next();
		System.out.println(pool.next());
		pool.close();
		
		//Assert.assertEquals(nbIncrementsWanted, value.intValue());
	}
	
	
}
