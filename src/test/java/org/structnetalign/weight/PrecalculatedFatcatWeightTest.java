/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 * @author dmyersturnbull
 */
package org.structnetalign.weight;

import static org.junit.Assert.assertEquals;

import org.junit.Test;



public class PrecalculatedFatcatWeightTest {

	private static final double PRECISION = 0.0000001;
	
	@Test
	public void test() throws Exception {
		String uniProtId1 = "P02761"; // 2a2g.A
		String uniProtId2 = "Q48422"; // 2a2l.A
		PrecalculatedFatcatWeight weight = new PrecalculatedFatcatWeight();
		double tmScore = weight.assignWeight(0, 1, uniProtId1, uniProtId2);
		assertEquals("Probability is wrong", 0.25580040494422324, tmScore, PRECISION);
	}
	
}
