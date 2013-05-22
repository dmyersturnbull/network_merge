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
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.NetworkUtils;
import org.structnetalign.weight.CeWeight;
import org.structnetalign.weight.ScopRelationWeight;
import org.structnetalign.weight.SimpleWeightManager;
import org.structnetalign.weight.WeightManager;

import psidev.psi.mi.xml.model.EntrySet;
import edu.uci.ics.jung.graph.UndirectedGraph;

public class PipelineManager {

	public static final double BETA = 1;
	public static final int N_CORES = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
	public static final double TAU = 0.5;
	public static final int XI = 5;
	public static final double ZETA = 0.7;

	private CrossingManager crossingManager;
	private MergeManager mergeManager;

	private int nCores;
	private boolean report = false;
	private WeightManager weightManager;
	private String initialConfidenceLabel = GraphInteractionAdaptor.INITIAL_CONFIDENCE_LABEL;
	public double defaultProbability = GraphInteractionAdaptor.DEFAULT_PROBABILITY;
	private boolean writeSteps = false;
	private boolean noMerge;
	private boolean noCross;

	private int xi = XI;
	private double zeta = ZETA;
	private double beta = BETA;
	private double tau = TAU;

	public boolean isNoMerge() {
		return noMerge;
	}

	public void setNoMerge(boolean noMerge) {
		this.noMerge = noMerge;
	}

	public boolean isNoCross() {
		return noCross;
	}

	public void setNoCross(boolean noCross) {
		this.noCross = noCross;
	}

	public double getDefaultProbability() {
		return defaultProbability;
	}

	public void setDefaultProbability(double defaultProbability) {
		this.defaultProbability = defaultProbability;
	}

	public double getBeta() {
		return beta;
	}

	public String getInitialConfidenceLabel() {
		return initialConfidenceLabel;
	}

	/**
	 * Initializes a new PipelineManager using the default parameters. Once this has been performed, setting of variables will have no effect.
	 */
	private void init() {
		SimpleWeightManager weightManager = new SimpleWeightManager();
		weightManager.setThreshold(tau);
		weightManager.add(new ScopRelationWeight());
		weightManager.add(new CeWeight());
		this.weightManager = weightManager;
		crossingManager = new SimpleCrossingManager(nCores, xi);
		mergeManager = new BronKerboschMergeManager();
	}

	public boolean isReport() {
		return report;
	}

	public boolean isWriteSteps() {
		return writeSteps;
	}

	/**
	 * Runs the entire pipeline.
	 */
	public void run(File input, File output) {

		init();

		String path = output.getParent();
		if (!path.endsWith(File.separator)) path += File.separator;

		CleverGraph graph;
		{
			// build the graph
			EntrySet entrySet = NetworkUtils.readNetwork(input);
			UndirectedGraph<Integer, InteractionEdge> interaction = GraphInteractionAdaptor.toGraph(entrySet, initialConfidenceLabel, defaultProbability);
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
		
		if (writeSteps) {
			GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File(path + "hom_weighted.graphml.xml"));
			GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File(path + "int_weighted.graphml.xml"));
		}

		// cross
		if (!noCross) {
			trimmer.trim(graph.getHomology(), tau);
			crossingManager.cross(graph);
		}
		if (writeSteps) {
			GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File(path + "hom_crossed.graphml.xml"));
			GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File(path + "int_crossed.graphml.xml"));
		}

		// merge
		if (!noMerge) {
			trimmer.trim(graph.getHomology(), zeta);
			mergeManager.merge(graph);
		}
		if (writeSteps) {
			GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File(path + "hom_merged.graphml.xml"));
			GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File(path + "int_merged.graphml.xml"));
		}

		crossingManager = null;
		mergeManager = null;
		trimmer = null;

		// now output
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		GraphInteractionAdaptor.modifyProbabilites(entrySet, graph.getInteraction());
		NetworkUtils.writeNetwork(entrySet, output);

	}

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public void setCrossingManager(CrossingManager crossingManager) {
		this.crossingManager = crossingManager;
	}

	public void setInitialConfidenceLabel(String initialConfidenceLabel) {
		this.initialConfidenceLabel = initialConfidenceLabel;
	}

	public void setMergeManager(MergeManager mergeManager) {
		this.mergeManager = mergeManager;
	}

	public void setNCores(int nCores) {
		this.nCores = nCores;
	}

	public void setReport(boolean report) {
		this.report = report;
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

	public void setWriteSteps(boolean writeSteps) {
		this.writeSteps = writeSteps;
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
