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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A really simple {@link WeightCreator} that tries, in order:
 * <ol>
 * <li>{@link ScopWeight}</li>
 * <li>{@link NeedlemanWunschWeight}</li>
 * <li>{@link CeWeight}</li>
 * </ol>
 * @author dmyersturnbull
 *
 */
public class SimpleWeightCreator implements WeightCreator {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	@Override
	public Weight nextWeight(int a, int b, String uniProtIdA, String uniProtIdB, int n, boolean isFail, Class<? extends Weight> failed) {

		if (!isFail) return null;
		
		Weight weight = null;

		// try SCOP
		if (n == 1) {
			weight = init(new ScopWeight(), a, b, uniProtIdA, uniProtIdB);
		}

		// try NW
		if (n == 2) {
			weight = init(new NeedlemanWunschWeight(), a, b, uniProtIdA, uniProtIdB);
		}
		
		// try pre-calculated FATCAT
		if (n == 3) {
			weight = init(new PrecalculatedFatcatWeight(), a, b, uniProtIdA, uniProtIdB);
		}

		// try CE
		if (n == 4) {
			weight = init(new CeWeight(), a, b, uniProtIdA, uniProtIdB);
		}
		
		return weight;
	}

	private Weight init(Weight weight, int a, int b, String uniProtIdA, String uniProtIdB) {
		try {
			weight.setIds(a, b, uniProtIdA, uniProtIdB);
			return weight;
		} catch (Exception e) {
			logger.debug("Couldn't create weight " + weight.getClass().getSimpleName() + " for (" + uniProtIdA + ", " + uniProtIdB + ")");
			return null;
		}
	}

	@Override
	public List<Weight> initialWeights(int a, int b, String uniProtIdA, String uniProtIdB) {
		List<Weight> list = new ArrayList<Weight>(1);
		Weight scop = init(new ScopWeight(), a, b, uniProtIdA, uniProtIdB);
		list.add(scop);
		return list;
	}

}
