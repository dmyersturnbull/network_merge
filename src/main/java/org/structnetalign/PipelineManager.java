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
package org.structnetalign;

import java.io.File;
import java.util.Map;

import org.structnetalign.cross.CrossingManager;
import org.structnetalign.cross.SimpleCrossingManager;
import org.structnetalign.merge.BronKerboschMergeManager;
import org.structnetalign.merge.MergeManager;
import org.structnetalign.util.EdgeTrimmer;
import org.structnetalign.util.EdgeWeighter;
import org.structnetalign.util.GraphInteractionAdaptor;
import org.structnetalign.util.NetworkUtils;
import org.structnetalign.weight.CeWeight;
import org.structnetalign.weight.ScopRelationWeight;
import org.structnetalign.weight.SimpleWeightManager;
import org.structnetalign.weight.WeightManager;

import psidev.psi.mi.xml.model.EntrySet;
import edu.uci.ics.jung.graph.UndirectedGraph;

public class PipelineManager {

	private WeightManager weightManager;
	private CrossingManager crossingManager;
	private MergeManager mergeManager;

	private int nCores;
	
	public static final int N_CORES = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
	public static final int XI = 5;
	public static final double ZETA = 0.7;
	public static final double TAU = 0.5;
	public static final double BETA = 1;
	
	private int xi = XI;
	private double zeta = ZETA;
	private double tau = TAU;
	
	public void init() {
		init(Math.max(Runtime.getRuntime().availableProcessors() - 1, 1));
	}

	/**
	 * Creates a new PipelineManager using the default parameters.
	 */
	public void init(int nCores) {
		this.nCores = nCores;
		SimpleWeightManager weightManager = new SimpleWeightManager();
		weightManager.setThreshold(tau);
		weightManager.add(new ScopRelationWeight());
		weightManager.add(new CeWeight());
		this.weightManager = weightManager;
		crossingManager = new SimpleCrossingManager(nCores, xi);
		mergeManager = new BronKerboschMergeManager();
	}

	/**
	 * Runs the entire pipeline.
	 */
	public void run(File input, File output) {

		CleverGraph graph;
		{
			// build the graph
			EntrySet entrySet = NetworkUtils.readNetwork(input);
			UndirectedGraph<Integer, InteractionEdge> interaction = GraphInteractionAdaptor.toGraph(entrySet, 0.5);
			graph = new CleverGraph(interaction);

			// assign weights
			Map<Integer, String> uniProtIds = NetworkUtils.getUniProtIds(entrySet);
			weightManager.assignWeights(graph, uniProtIds);
		}
		System.gc();

		// make a trimmer
		EdgeWeighter<HomologyEdge> weighter = new EdgeWeighter<HomologyEdge>() {
			@Override
			public double getWeight(HomologyEdge e) {
				return e.getWeight();
			}
		};
		EdgeTrimmer<Integer, HomologyEdge> trimmer = new EdgeTrimmer<>(weighter);

		// cross
		trimmer.trim(graph.getHomology(), tau);
		crossingManager.cross(graph);

		// merge
		trimmer.trim(graph.getHomology(), zeta);
		mergeManager.merge(graph);
		
		crossingManager = null; mergeManager = null; trimmer = null;

		// now output
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		GraphInteractionAdaptor.modifyProbabilites(entrySet, graph.getInteraction());
		NetworkUtils.writeNetwork(entrySet, output);

	}

	public void setCrossingManager(CrossingManager crossingManager) {
		this.crossingManager = crossingManager;
	}

	public void setMergeManager(MergeManager mergeManager) {
		this.mergeManager = mergeManager;
	}

	public void setNCores(int nCores) {
		this.nCores = nCores;
	}

	/**
	 * @param tau
	 *            The minimum threshold to apply to homology edges before doing crossing.
	 */
	public void setTau(double tau) {
		this.tau = tau;
	}

	public void setWeightManager(WeightManager weightManager) {
		this.weightManager = weightManager;
	}

	/**
	 * @param xi
	 *            The maximum search depth for traversal during crossing.
	 */
	public void setXi(int xi) {
		this.xi = xi;
	}

	/**
	 * @param zeta
	 *            The minimum threshold to apply to homology edges before doing merging
	 */
	public void setZeta(double zeta) {
		this.zeta = zeta;
	}

}
