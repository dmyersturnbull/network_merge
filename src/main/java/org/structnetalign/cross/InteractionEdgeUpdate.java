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

package org.structnetalign.cross;

import org.structnetalign.InteractionEdge;

public class InteractionEdgeUpdate {

	private final InteractionEdge rootInteraction;
	
	private final int interactorA;
	private final int interactorB;
	
	private int nUpdates;
	
	public int getnUpdates() {
		return nUpdates;
	}

	public void setnUpdates(int nUpdates) {
		this.nUpdates = nUpdates;
	}

	public InteractionEdgeUpdate(InteractionEdge rootInteraction, int interactorA, int interactorB) {
		super();
		this.rootInteraction = rootInteraction;
		this.interactorA = interactorA;
		this.interactorB = interactorB;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public InteractionEdge getRootInteraction() {
		return rootInteraction;
	}

	public int getInteractorA() {
		return interactorA;
	}

	public int getInteractorB() {
		return interactorB;
	}

	private double score;
	
}
