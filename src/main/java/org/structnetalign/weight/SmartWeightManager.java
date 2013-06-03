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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.ReportGenerator;

/**
 * An intelligent multithreaded {@link WeightManager} that uses sequence information if and only if the corresponding
 * structural information is missing.
 * This is a pretty bad implementation, since the weights it assigns don't correspond to probabilities (and can even
 * exceed 1).
 * @author dmyersturnbull
 * 
 */
public class SmartWeightManager implements WeightManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private int nCores;

	private double beta = 1;

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public SmartWeightManager(int nCores) {
		super();
		this.nCores = nCores;
	}

	private static final NumberFormat nf = new DecimalFormat();
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}

	@Override
	public void assignWeights(CleverGraph graph, Map<Integer, String> uniProtIds) {

		if (ReportGenerator.getInstance() != null) {
			ReportGenerator.getInstance().putInWeighted("manager", this.getClass().getSimpleName());
		}

		// this is a bit annoying, but we need a map going from UniProt Ids to our Ids
		// this is only because we're doing this concurrently
		Map<String, Integer> graphIds = new HashMap<>();
		for (Map.Entry<Integer, String> entry : uniProtIds.entrySet()) {
			graphIds.put(entry.getValue(), entry.getKey());
		}

		// make a thread pool
		logger.info("Starting weight assignment with " + nCores + " cores");
		ExecutorService pool = Executors.newFixedThreadPool(nCores);

		try {

			CompletionService<WeightResult> completion = new ExecutorCompletionService<>(pool);
			List<Future<WeightResult>> futures = new ArrayList<>();

			// let's submit the jobs
			// iterate over all pairs of vertices
			for (int a : graph.getVertices()) {
				for (int b : graph.getVertices()) {

					if (a >= b) {
						continue; // homology had damn well better be reflexive and symmetric!
					}

					final String uniProtIdA = uniProtIds.get(a);
					final String uniProtIdB = uniProtIds.get(b);

					if (uniProtIdA == null) {
						logger.error("Could not get UniProt Id for Id#" + a);
						continue;
					}
					if (uniProtIdB == null) {
						logger.error("Could not get UniProt Id for Id#" + b);
						continue;
					}

					logger.trace("Weighting " + uniProtIdA + " against " + uniProtIdB + " (" + a + ", " + b + ")");

					// let's get weight from alignment
					AlignmentWeight alignment;
					try {
						// try to use structure
						alignment = new NeedlemanWunschWeight();
						alignment.setIds(uniProtIdA, uniProtIdB);
					} catch (WeightException e) {
						logger.warn("Couldn't get CE weight for " + uniProtIdA + " against " + uniProtIdB + " (" + a + ", " + b + ")", e);
						// okay, try to use sequence
						alignment = new NeedlemanWunschWeight();
						try {
							alignment.setIds(uniProtIdA, uniProtIdB);
						} catch (WeightException e1) {
							logger.warn("Couldn't get alignment-based weight for " + uniProtIdA + " against " + uniProtIdB + " (" + a + ", " + b + ")", e1);
							alignment = null;
						}
					}

					// let's get weight from a database
					RelationWeight relation;
					try {
						// try to use structure
						relation = new ScopRelationWeight();
						relation.setIds(uniProtIdA, uniProtIdB);
					} catch (WeightException e) {
						logger.warn("Couldn't get SCOP weight for " + uniProtIdA + " against " + uniProtIdB + " (" + a + ", " + b + ")", e);
						// okay, try to use sequence
						relation = new PfamWeight();
						try {
							relation.setIds(uniProtIdA, uniProtIdB);
						} catch (WeightException e1) {
							logger.warn("Couldn't get relation-based weight for " + uniProtIdA + " against " + uniProtIdB + " (" + a + ", " + b + ")", e1);
							relation = null;
						}
					}

					// now submit
					if (alignment != null && !Double.isInfinite(beta)) { // beta == infinity means we're not using alignment
						logger.debug("Running alignment " + alignment.getClass().getSimpleName() + " for " + uniProtIdA + " against " + uniProtIdB + " (" + a + ", " + b + ")");
						Future<WeightResult> alignmentWeight = completion.submit(alignment);
						futures.add(alignmentWeight);
					}
					if (relation != null) {
						logger.debug("Running relation " + relation.getClass().getSimpleName() + " for " + uniProtIdA + " against " + uniProtIdB + " (" + a + ", " + b + ")");
						Future<WeightResult> relationWeight = completion.submit(relation);
						futures.add(relationWeight);
					}

				}
			}

			logger.info("Submitted " + futures.size() + " jobs to " + nCores + " cores");


			/*
			 *  Now respond to completion.
			 */

			int nUpdates = 0;

			int createdIndex = 0; // there shouldn't be any homology edges yet
			forfutures: for (Future<WeightResult> future : futures) {

				Double weight = null;
				Integer vertexA = null, vertexB = null;

				try {

					// We should do this in case the job gets interrupted
					// Sometimes the OS or JVM might do this
					// Use the flag instead of future == null because future.get() may actually return null
					while (weight == null) {
						try {
							WeightResult result = future.get();
							weight = result.getWeight();
							vertexA = graphIds.get(result.getA());
							vertexB = graphIds.get(result.getB());
							logger.trace("Job (" + vertexA + ", " + vertexB + ") returned with weight " + nf.format(weight));
							if (weight == 0) {
								continue forfutures; // don't both updating with 0
							}
							if (result.getSubmitter().isInstance(RelationWeight.class)) {
								weight *= beta; // scale database results by beta as per description
							}
						} catch (InterruptedException e1) {
							logger.warn("A thread was interrupted while waiting to get a weight. Retrying.", e1);
						}
					}

				} catch (ExecutionException e) {

					// we can try this again if it's structural
					if (e.getCause() != null && e.getCause() instanceof WeightException) {
						WeightException myE = (WeightException) e.getCause();
						if (myE.isStructure()) {
							String myA = myE.getA();
							String myB = myE.getB();
							if (myE.isAlignment()) {
								logger.warn("Structure-based alignment weight failed for (" + myA + ", " + myB + "). Attempting to use a sequence alignment.", e);
								try {
									AlignmentWeight alignment = new NeedlemanWunschWeight();
									alignment.setIds(myA, myB);
									completion.submit(alignment);
								} catch (WeightException e1) {

								}
							} else {
								logger.warn("Structure-based relation weight failed for (" + myA + ", " + myB + ") Attempting to use sequence a relation.", e);
								try {
									RelationWeight relation = new PfamWeight();
									relation.setIds(myA, myB);
									completion.submit(relation);
								} catch (WeightException e1) {

								}
							}
						} else {
							logger.error("Encountered an error using sequence alignment.", e);
						}
					} else {
						logger.error("Encountered an unknown error trying to get a weight.", e);
					}

					continue; // we can't process this job

				}

				// everything is ok; now update or add the edge

				Collection<Integer> vertices = Arrays.asList(vertexA, vertexB);

				// there may already be an edge there
				HomologyEdge existing = graph.getHomology().findEdge(vertexA, vertexB);
				if (existing != null) {
					// (a+b-ab) + c - c*(a+b-ab) = a + b + c - ab - ac - bc + abc
					existing.setWeight(existing.getWeight() + weight - existing.getWeight() * weight);
					logger.debug("[" + ((double) createdIndex) / ((double) (futures.size() + createdIndex)) + "] Updated homology edge (" + vertexA + ", " + vertexB + ", " + nf.format(existing.getWeight()) + ") with weight " + nf.format(weight));
				} else {
					HomologyEdge edge = new HomologyEdge(createdIndex++, weight);
					graph.addHomologies(edge, vertices);
					logger.debug("Added homology edge (" + vertexA + ", " + vertexB + ", " + nf.format(weight) + ")");
				}
				nUpdates++;

			}

			logger.info("Added " + graph.getHomologyCount() + " homology edges");
			if (ReportGenerator.getInstance() != null) {
				ReportGenerator.getInstance().putInWeighted("n_updates", nUpdates);
			}

			int maxHomologyDegree = 0;
			for (int v : graph.getVertices()) {
				int x = graph.getHomology().getIncidentEdges(v).size();
				if (x > maxHomologyDegree) maxHomologyDegree = x;
			}
			if (ReportGenerator.getInstance() != null) {
				ReportGenerator.getInstance().putInWeighted("max_homology_degree", maxHomologyDegree);
			}

			int maxInteractionDegree = 0;
			for (int v : graph.getVertices()) {
				int x = graph.getInteraction().getIncidentEdges(v).size();
				if (x > maxInteractionDegree) maxInteractionDegree = x;
			}
			if (ReportGenerator.getInstance() != null) {
				ReportGenerator.getInstance().putInWeighted("max_interaction_degree", maxInteractionDegree);
			}


		} finally {
			pool.shutdownNow();

			int count = Thread.activeCount()-1;
			if (count > 0) {
				logger.warn("There are " + count + " lingering threads");
			}
		}
	}

}
