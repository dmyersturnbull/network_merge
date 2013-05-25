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
package org.structnetalign.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import psidev.psi.mi.xml.model.EntrySet;



public class NetworkUtilsTest {

	private static final String RESOURCE_DIR = "src/test/resources/util/";
	
	@Test
	public void testGetUniProtIds() {
		File input = new File(RESOURCE_DIR + "with_uniprot_refs.psimi.xml");
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		Map<Integer,String> map = NetworkUtils.getUniProtIds(entrySet);
		assertEquals("Q20136", map.get(26463));
		assertEquals("O44750", map.get(26588));
		assertEquals("Q95ZX3", map.get(24500));
		assertEquals("Q9U296", map.get(26062));
		assertEquals("Q9GSQ0", map.get(25926));
		assertEquals("O44606", map.get(25031));
		assertEquals("Q22311", map.get(26113));
		assertEquals("Q11093", map.get(26756));
		assertEquals("Q20712", map.get(25066)); // in the middle
		assertEquals("P43509", map.get(25329)); // very last
	}
}
