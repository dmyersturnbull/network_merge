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
 * @author dmyersturnbull
 */
package org.structnetalign;

import org.structnetalign.cross.CrossingManager;
import org.structnetalign.merge.MergeManager;
import org.structnetalign.weight.WeightManager;

public class PipelineManager {

	private CrossingManager crossingManager;
	private MergeManager mergeManager;
	private WeightManager weightManager;
	public CrossingManager getCrossingManager() {
		return crossingManager;
	}
	public void setCrossingManager(CrossingManager crossingManager) {
		this.crossingManager = crossingManager;
	}
	public MergeManager getMergeManager() {
		return mergeManager;
	}
	public void setMergeManager(MergeManager mergeManager) {
		this.mergeManager = mergeManager;
	}
	public WeightManager getWeightManager() {
		return weightManager;
	}
	public void setWeightManager(WeightManager weightManager) {
		this.weightManager = weightManager;
	}
	
	public PipelineManager() {
		
	}
	
	public void run() {
		
	}
	
}
