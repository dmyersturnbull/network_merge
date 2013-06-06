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

import java.util.concurrent.Callable;

/**
 * Something that assigns a weight describing the degree or probability of homology between two macromolecules. Calling
 * code should use either {@link #setIds(int, int, String, String)} and then {@link #call()}, or just
 * {@link #assignWeight(int, int, String, String)}.
 * 
 * @author dmyersturnbull
 */
public interface Weight extends Callable<WeightResult> {

	/**
	 * A convenience method for single runs. Equivalent to calling {@link #setIds(String, String)} followed by
	 * {@link #call()}.
	 * 
	 * @see #setIds(int, int, String, String)
	 */
	double assignWeight(int v1, int v2, String uniProtId1, String uniProtId2) throws Exception;

	/**
	 * 
	 * @param v1
	 *            The PSI-MI XML Id of the first vertex
	 * @param v2
	 *            The PSI-MI XML Id of the second vertex
	 * @throws WeightException
	 *             If this Weight could not be initialized; calling code should handle this well
	 */
	void setIds(int v1, int v2, String uniProtId1, String uniProtId2) throws WeightException;

}
