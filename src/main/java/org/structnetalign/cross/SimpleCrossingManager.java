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
 * @author dmyersturnbull
 */
package org.structnetalign.cross;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.InteractionEdge;

public class SimpleCrossingManager implements CrossingManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");
	private int maxDepth;
	private int nCores;

	private static NumberFormat nf = new DecimalFormat();
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}
	
	public SimpleCrossingManager(int nCores, int maxDepth) {
		this.nCores = nCores;
		this.maxDepth = maxDepth;
	}

	@Override
	public void cross(CleverGraph graph) {
		
		ExecutorService pool = Executors.newFixedThreadPool(nCores);
		
		// depressingly, this used to be List<Future<Pair<Map<Integer,Double>>>>
		// I'm glad that's no longer the case
		CompletionService<InteractionUpdate> completion = new ExecutorCompletionService<>(pool);
		List<Future<InteractionUpdate>> futures = new ArrayList<>();
		
		// submit the jobs
		for (InteractionEdge interaction : graph.getInteraction().getEdges()) {
			HomologySearchJob job = new HomologySearchJob(interaction, graph);
			job.setMaxDepth(maxDepth);
			Future<InteractionUpdate> result = completion.submit(job);
			futures.add(result);
		}

		/*
		 * We'll make a list of updates to do when we're finished.
		 * Otherwise, we can run into some ugly concurrency issues and get the wrong answer.
		 */
		HashMap<InteractionEdge, Double> edgesToUpdate = new HashMap<>();
		
		for (Future<InteractionUpdate> future : futures) {
			
			// now wait for completion
			InteractionUpdate update = null;
			try {
				// We should do this in case the job gets interrupted
				// Sometimes the OS or JVM might do this
				// Use the flag instead of future == null because future.get() may actually return null
				while (update == null) {
					try {
						update = future.get();
					} catch (InterruptedException e) {
						logger.warn("A thread was interrupted while waiting to get interaction udpate. Retrying.", e);
						continue;
					}
				}
			} catch (ExecutionException e) {
				logger.error("Encountered an error trying to update an interaction. Skipping interaction.", e);
				continue;
			}
			
			// we have an update to make!
			InteractionEdge edge = update.getRootInteraction(); // don't make a copy here!!
			edgesToUpdate.put(edge, edge.getWeight() + update.getScore() - edge.getWeight() * update.getScore());
			logger.debug("Updated interaction " + edge.getId() + " to " + nf.format(edge.getWeight()));
		}

		/*
		 * Now that the multithreaded part has finished, we can update the interactions.
		 */
		for (InteractionEdge edge : edgesToUpdate.keySet()) {
			edge.setWeight(edgesToUpdate.get(edge));
		}
		
	}

}
