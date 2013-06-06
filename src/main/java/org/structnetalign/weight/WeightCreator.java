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

import java.util.List;

public interface WeightCreator {

	/**
	 * Get a list of weights to apply on {@code a} and {@code b}. All of these weights will be attempted, and
	 * {@link #nextWeight(int, int, String, String, int)} with {@code n=1} will be called for each weight that fails.
	 */
	List<Weight> initialWeights(int a, int b, String uniProtIdA, String uniProtIdB);

	/**
	 * Get a Weight to use for {@code a} and {@code b}, assuming that the previous {@code n} attempts failed, where
	 * {@code n=0} corresponds to {@link #initialWeights(int, int, String, String)} and {@code n=1} to the first
	 * failure. This method should <em>not</em> keep track of number of failures; that is the responsibility of calling
	 * code.
	 * @param failed The class of the {@link Weight} that failed or null if its unknown
	 */
	Weight nextWeight(int a, int b, String uniProtIdA, String uniProtIdB, int n, Class<? extends Weight> failed);

}
