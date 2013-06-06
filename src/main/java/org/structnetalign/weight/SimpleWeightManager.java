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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;

/**
 * A simple {@link WeightManager} that keeps a list of {@link Weight Weights} and sums over each weight. If a Weight
 * fails, it simply adds 0.
 * 
 * @author dmyersturnbull
 * 
 */
public class SimpleWeightManager implements WeightManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private List<Double> coefficients;
	private List<Weight> weights;

	public SimpleWeightManager() {
		super();
		weights = new ArrayList<Weight>();
		coefficients = new ArrayList<Double>();
	}

	public SimpleWeightManager(List<Weight> weights, List<Double> coefficients) {
		super();
		this.weights = weights;
		this.coefficients = coefficients;
	}

	public boolean add(Weight e, double coefficient) {
		coefficients.add(coefficient);
		return weights.add(e);
	}

	private static final NumberFormat nf = new DecimalFormat();
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}

	@Override
	public void assignWeights(CleverGraph graph, Map<Integer, String> uniProtIds) {

		int createdIndex = 0; // there shouldn't be any homology edges yet

		for (int a : graph.getVertices()) {
			for (int b : graph.getVertices()) {

				if (a >= b) continue; // homology had damn well better be reflexive and symmetric!

				double score = 0;

				for (int i = 0; i < weights.size(); i++) {
					String sa = uniProtIds.get(a);
					String sb = uniProtIds.get(b);
					try {
						double updateScore = coefficients.get(i) * weights.get(i).assignWeight(a, b, sa, sb);
						score += updateScore - score * updateScore;
					} catch (Exception e) {
						// totally okay; just don't add
						logger.debug("Couldn't get a weight for " + a + " against " + b, e);
					}
				}

				Collection<Integer> vertices = Arrays.asList(a, b);
				HomologyEdge edge = new HomologyEdge(createdIndex++, score);
				graph.addHomologies(edge, vertices);
				logger.debug("Added homology edge (" + a + ", " + b + ", " + nf.format(score) + ")");
				
			}
		}

	}
	public void setWeights(List<Weight> weights) {
		this.weights = weights;
	}

}
