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

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class WeightResult {

	private static final NumberFormat nf = new DecimalFormat();
	
	private String a;

	private String b;

	private Class<? extends Weight> submitter;

	private double weight;

	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}

	public WeightResult(double weight, String a, String b, Class<? extends Weight> submitter) {
		super();
		this.weight = weight;
		this.a = a;
		this.b = b;
		this.submitter = submitter;
	}

	public String getA() {
		return a;
	}

	public String getB() {
		return b;
	}

	public double getWeight() {
		return weight;
	}

	public void setA(String a) {
		this.a = a;
	}

	public void setB(String b) {
		this.b = b;
	}

	public Class<? extends Weight> getSubmitter() {
		return submitter;
	}

	public void setSubmitter(Class<? extends Weight> submitter) {
		this.submitter = submitter;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return submitter.getSimpleName() + ": (" + a + ", " + b + ") --> " + nf.format(weight);
	}

}
