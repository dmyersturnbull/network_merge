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
package org.structnetalign;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class HomologyEdge implements Edge {

	public static enum Type {
		SEQUENCE_ALIGNMENT, SEQUENCE_DATABASE, STRUCTURAL_ALIGNMENT, STRUCTURAL_DATABASE;
	}

	private static NumberFormat nf = new DecimalFormat();

	private int id;

	private double weight;

	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}

	public HomologyEdge() {

	}

	public HomologyEdge(HomologyEdge edge) {
		super();
		id = edge.id;
		weight = edge.weight;
	}

	public HomologyEdge(int id, double weight) {
		super();
		this.id = id;
		this.weight = weight;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		HomologyEdge other = (HomologyEdge) obj;
		if (id != other.id) return false;
		return true;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public double getWeight() {
		return weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "Hom(" + id + ", " + nf.format(weight) + ")";
	}

}
