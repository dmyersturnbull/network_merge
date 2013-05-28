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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.cross.CrossingManager;
import org.structnetalign.cross.SimpleCrossingManager;
import org.structnetalign.merge.ConcurrentBronKerboschMergeManager;
import org.structnetalign.merge.MergeManager;
import org.structnetalign.util.EdgeTrimmer;
import org.structnetalign.util.EdgeWeighter;
import org.structnetalign.util.GraphInteractionAdaptor;
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.NetworkUtils;
import org.structnetalign.weight.SmartWeightManager;
import org.structnetalign.weight.WeightManager;

import psidev.psi.mi.xml.model.EntrySet;
import edu.uci.ics.jung.graph.UndirectedGraph;

public class PipelineManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

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
		SmartWeightManager weightManager = new SmartWeightManager(nCores);
		weightManager.setBeta(beta);
		this.weightManager = weightManager;
		crossingManager = new SimpleCrossingManager(nCores, xi);
		mergeManager = new ConcurrentBronKerboschMergeManager(nCores);
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

		int startTime = (int) (System.currentTimeMillis() / 1000L);
		
		init();

		// handle reporting
		// always do this even if we're not generating the report, since MergeManager etc. needs it
		String timestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String path = output.getParent() + File.separator + "report-" + timestamp + File.separator;
		if (report || writeSteps) new File(path).mkdir(); // don't make a new directory if we're not reporting
		File reportFile = new File(path + "report.html");
		ReportGenerator.setInstance(new ReportGenerator(reportFile));
		if (report) {
			ReportGenerator.getInstance().put("n_cores", nCores);
			ReportGenerator.getInstance().put("beta", beta);
			ReportGenerator.getInstance().put("tau", tau);
			ReportGenerator.getInstance().put("zeta", zeta);
			ReportGenerator.getInstance().put("xi", xi);
		}

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
		
		if (report) {
			ReportGenerator.getInstance().saveWeighted(graph);
		}
		if (writeSteps) {
			GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File(path + "hom_weighted.graphml.xml"));
			GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File(path + "int_weighted.graphml.xml"));
		}
		
		EdgeTrimmer<Integer, HomologyEdge> trimmer = new EdgeTrimmer<>(weighter);
		
		// cross
		trimmer.trim(graph.getHomology(), tau);
		if (!noCross) {
			crossingManager.cross(graph);
		}
		if (writeSteps) {
			GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File(path + "hom_crossed.graphml.xml"));
			GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File(path + "int_crossed.graphml.xml"));
		}
		if (report) {
			ReportGenerator.getInstance().saveCrossed(graph);
		}
		
		// merge
		trimmer.trim(graph.getHomology(), zeta);
		if (!noMerge) {
			mergeManager.merge(graph);
		}
		if (writeSteps) {
			GraphMLAdaptor.writeHomologyGraph(graph.getHomology(), new File(path + "hom_merged.graphml.xml"));
			GraphMLAdaptor.writeInteractionGraph(graph.getInteraction(), new File(path + "int_merged.graphml.xml"));
		}
		
		if (report) {
			ReportGenerator.getInstance().saveMerged(graph);
		}

		crossingManager = null;
		mergeManager = null;
		trimmer = null;

		// now output
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		GraphInteractionAdaptor.modifyProbabilites(entrySet, graph.getInteraction());
		NetworkUtils.writeNetwork(entrySet, output);

		int endTime = (int) (System.currentTimeMillis() / 1000L);
		if (report) {
			ReportGenerator.getInstance().put("time_taken", (endTime - startTime));
			ReportGenerator.getInstance().write();
		}
		
		int count = Thread.activeCount()-1;
		if (count > 0) {
			logger.warn("There are " + count + " lingering threads. Exiting anyway.");
			System.exit(0);
		}
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
