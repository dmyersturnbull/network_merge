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

/**
 * A failure to initialize a {@link Weight}. Calling code needs to handle these well.
 * 
 * @author dmyersturnbull
 */
@SuppressWarnings("serial")
public class WeightException extends Exception {

	private int a;
	private int b;
	private String uniProtIdA;
	private String uniProtIdB;
	private boolean isAlignment;
	private boolean isStructure;

	public WeightException() {
		super();
	}

	public WeightException(String message, int a, int b, String uniProtIdA, String uniProtIdB, boolean isAlignment, boolean isStructure) {
		super(message);
		this.a = a;
		this.b = b;
		this.uniProtIdA = uniProtIdA;
		this.uniProtIdB = uniProtIdB;
		this.isAlignment = isAlignment;
		this.isStructure = isStructure;
	}

	public WeightException(String message, Throwable cause, int a, int b, String uniProtIdA, String uniProtIdB, boolean isAlignment, boolean isStructure) {
		super(message, cause);
		this.a = a;
		this.b = b;
		this.uniProtIdA = uniProtIdA;
		this.uniProtIdB = uniProtIdB;
		this.isAlignment = isAlignment;
		this.isStructure = isStructure;
	}

	public WeightException(Throwable cause, int a, int b, String uniProtIdA, String uniProtIdB, boolean isAlignment, boolean isStructure) {
		super(cause);
		this.a = a;
		this.b = b;
		this.uniProtIdA = uniProtIdA;
		this.uniProtIdB = uniProtIdB;
		this.isAlignment = isAlignment;
		this.isStructure = isStructure;
	}

	public String getUniProtIdA() {
		return uniProtIdA;
	}

	public String getUniProtIdB() {
		return uniProtIdB;
	}

	public boolean isAlignment() {
		return isAlignment;
	}

	public boolean isStructure() {
		return isStructure;
	}

	public int getA() {
		return a;
	}

	public int getB() {
		return b;
	}

}
