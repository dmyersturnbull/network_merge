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

import static org.junit.Assert.*;

import org.junit.Test;



public class NeedlemanWunschWeightTest {

	private static final double PRECISION = 0.000001;
	
	@Test
	public void test() throws Exception {
		NeedlemanWunschWeight weight = new NeedlemanWunschWeight();
		/*
		 * Ran this on EBI.
		 * Make sure to set extend penalty to 1, and to check gap end penalty.
		 * EBU reports 16.9% identity.
		 */
		double prob = weight.assignWeight("P02185", "P00720");
		assertEquals(0.39176030, prob, PRECISION);
	}
	
}
