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

/**
 * An intelligent multithreaded {@link WeightManager} that uses sequence information if and only if the corresponding
 * structural information is missing.
 * 
 * @author dmyersturnbull
 * 
 */
public class SmartWeightManager implements WeightManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private int nCores;

	private double threshold;

	public SmartWeightManager(int nCores, double threshold) {
		super();
		this.nCores = nCores;
		this.threshold = threshold;
	}

	@Override
	public void assignWeights(CleverGraph graph, Map<Integer, String> uniProtIds) {

		// this is a bit annoying, but we need a map going from UniProt Ids to our Ids
		// this is only because we're doing this concurrently
		Map<String, Integer> graphIds = new HashMap<>();
		for (Map.Entry<Integer, String> entry : uniProtIds.entrySet()) {
			graphIds.put(entry.getValue(), entry.getKey());
		}

		// make a thread pool
		logger.info("Starting weight assignment with " + nCores + " cores");
		ExecutorService pool = Executors.newFixedThreadPool(nCores);
		CompletionService<WeightResult> completion = new ExecutorCompletionService<>(pool);
		List<Future<WeightResult>> futures = new ArrayList<>();

		// let's submit the jobs
		// iterate over all pairs of vertices
		int i = 0;
		for (int a : graph.getVertices()) {
			int j = 0;
			for (int b : graph.getVertices()) {

				if (i <= j) continue; // homology had damn well better be reflexive!

				final String uniProtIdA = uniProtIds.get(a);
				final String uniProtIdB = uniProtIds.get(b);

				// let's get weight from alignment
				AlignmentWeight alignment;
				try {
					// try to use structure
					alignment = new CeWeight();
					alignment.setIds(uniProtIdA, uniProtIdB);
				} catch (WeightException e) {
					// okay, try to use sequence
					logger.debug("Using sequence alignment for " + uniProtIdA + " against " + uniProtIdB, e);
					alignment = new NeedlemanWunschWeight();
					try {
						alignment.setIds(uniProtIdA, uniProtIdB);
					} catch (WeightException e1) {
						logger.warn("Couldn't get alignment-based weight for " + uniProtIdA + " against " + uniProtIdB, e1);
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
					// okay, try to use sequence
					logger.debug("Using sequence relation for " + uniProtIdA + " against " + uniProtIdB, e);
					relation = new PfamWeight();
					try {
						relation.setIds(uniProtIdA, uniProtIdB);
					} catch (WeightException e1) {
						logger.warn("Couldn't get relation-based weight for " + uniProtIdA + " against " + uniProtIdB, e1);
						relation = null;
					}
				}

				// now submit
				if (alignment != null) {
					Future<WeightResult> alignmentWeight = completion.submit(alignment);
					futures.add(alignmentWeight);
				}
				if (relation != null) {
					Future<WeightResult> relationWeight = completion.submit(relation);
					futures.add(relationWeight);
				}

				j++;
			}
			i++;
		}

		logger.info("Submitted " + futures.size() + " jobs to " + nCores + " cores");

		// now respond to completion
		int createdIndex = 0;
		for (Future<WeightResult> future : futures) {
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
					} catch (InterruptedException e1) {
						logger.warn("A thread was interrupted while waiting to get a weight. Retrying.", e1);
					}
				}
			} catch (ExecutionException e) {
				logger.error("Encountered an error trying to get a weight. Skipping.", e);
				continue;
			}

			if (weight >= threshold) {

				logger.debug("Adding homology edge (" + vertexA + "," + vertexB + "," + weight + ")");
				Collection<Integer> vertices = Arrays.asList(vertexA, vertexB);

				// there may already be an edge there
				HomologyEdge existing = graph.getHomology().findEdge(vertexA, vertexB);
				if (existing != null) {
					existing.setWeight(existing.getWeight() + weight);
				} else {
					HomologyEdge edge = new HomologyEdge(createdIndex++, weight);
					graph.addHomologies(edge, vertices);
				}

			}
		}

		logger.info("Added " + graph.getHomologyCount() + " homology edges");

	}

}
