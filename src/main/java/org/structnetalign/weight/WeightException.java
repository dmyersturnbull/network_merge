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

	private String a;
	private String b;
	private boolean isAlignment;
	private boolean isStructure;

	public WeightException() {
		super();
	}

	public WeightException(String message, String a, String b, boolean isAlignment, boolean isStructure) {
		super(message);
		this.a = a;
		this.b = b;
		this.isAlignment = isAlignment;
		this.isStructure = isStructure;
	}

	public WeightException(String message, Throwable cause, String a, String b, boolean isAlignment, boolean isStructure) {
		super(message, cause);
		this.a = a;
		this.b = b;
		this.isAlignment = isAlignment;
		this.isStructure = isStructure;
	}

	public WeightException(Throwable cause, String a, String b, boolean isAlignment, boolean isStructure) {
		super(cause);
		this.a = a;
		this.b = b;
		this.isAlignment = isAlignment;
		this.isStructure = isStructure;
	}

	public String getA() {
		return a;
	}

	public String getB() {
		return b;
	}

	public boolean isAlignment() {
		return isAlignment;
	}

	public boolean isStructure() {
		return isStructure;
	}

}
