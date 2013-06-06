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

public class PfamWeight implements RelationWeight {

	private int v1;
	private int v2;

	private String uniProtId1;
	private String uniProtId2;
	
	@Override
	public void setIds(int v1, int v2, String uniProtId1, String uniProtId2) throws WeightException {
		this.v1 = v1;
		this.v2 = v2;
		this.uniProtId1 = uniProtId1;
		this.uniProtId2 = uniProtId2;
	}

	@Override
	public double assignWeight(int v1, int v2, String uniProtId1, String uniProtId2) throws Exception {
		setIds(v1, v2, uniProtId1, uniProtId2);
		return call().getWeight();
	}

	@Override
	public WeightResult call() throws Exception {
		// in all honesty, I won't finish this
		// but we can put a TODO here anyway
		return new WeightResult(0, v1, v2, uniProtId1, uniProtId2, this.getClass());
	}

}
