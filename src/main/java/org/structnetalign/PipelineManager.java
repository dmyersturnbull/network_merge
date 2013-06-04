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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.ReportGenerator.DegenerateSetEntry;
import org.structnetalign.ReportGenerator.UpdateTableEntry;
import org.structnetalign.cross.CrossingManager;
import org.structnetalign.cross.SimpleCrossingManager;
import org.structnetalign.merge.ConcurrentBronKerboschMergeManager;
import org.structnetalign.merge.MergeManager;
import org.structnetalign.merge.MergeUpdate;
import org.structnetalign.util.EdgeTrimmer;
import org.structnetalign.util.EdgeWeighter;
import org.structnetalign.util.GraphInteractionAdaptor;
import org.structnetalign.util.GraphMLAdaptor;
import org.structnetalign.util.IdentifierMapping;
import org.structnetalign.util.IdentifierMappingFactory;
import org.structnetalign.util.InteractionUpdate;
import org.structnetalign.util.NetworkUtils;
import org.structnetalign.weight.SmartWeightManager;
import org.structnetalign.weight.WeightManager;

import psidev.psi.mi.xml.model.EntrySet;
import edu.uci.ics.jung.graph.UndirectedGraph;

public class PipelineManager {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	public static final double BETA = Double.POSITIVE_INFINITY;
	public static final int N_CORES = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
	public static final double TAU = 0.5;
	public static final int XI = 2;
	public static final double ZETA = 0.7;

	private CrossingManager crossingManager;
	private MergeManager mergeManager;

	private int nCores;
	private boolean report = false;
	private WeightManager weightManager;
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

	public double getBeta() {
		return beta;
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
			UndirectedGraph<Integer, InteractionEdge> interaction = GraphInteractionAdaptor.toGraph(entrySet);
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
		List<MergeUpdate> merges = null;
		trimmer.trim(graph.getHomology(), zeta);
		if (!noMerge) {
			merges = mergeManager.merge(graph);
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
		Map<Integer,Integer> interactionsRemoved = new WeakHashMap<>();
		for (MergeUpdate update : merges) {
			for (InteractionEdge e : update.getInteractionEdges()) {
				interactionsRemoved.put(e.getId(), update.getV0());
			}
		}
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		List<InteractionUpdate> updates = GraphInteractionAdaptor.modifyProbabilites(entrySet, graph.getInteraction(), interactionsRemoved);
		NetworkUtils.writeNetwork(entrySet, output);

		int endTime = (int) (System.currentTimeMillis() / 1000L);
		if (report) {
			putUpdates(updates);
			putMerges(merges, entrySet);
			ReportGenerator.getInstance().put("time_taken", (endTime - startTime));
			ReportGenerator.getInstance().write();
		}
		
		int count = Thread.activeCount()-1;
		if (count > 0) {
			logger.warn("There are " + count + " lingering threads. Exiting anyway.");
			System.exit(0);
		}
	}
	
	private void putMerges(List<MergeUpdate> merges, EntrySet entrySet) {
		Map<Integer,String> uniProtIds = NetworkUtils.getUniProtIds(entrySet);
		final IdentifierMapping mapping = IdentifierMappingFactory.getMapping();
		List<DegenerateSetEntry> entries = new ArrayList<>();
		for (MergeUpdate update : merges) {
			DegenerateSetEntry entry = new DegenerateSetEntry();
			entry.v0 = update.getV0();
			entry.uniProtId0 = uniProtIds.get(update.getV0());
			for (int v : update.getVertices()) {
				entry.getIds().add(v);
				final String uniProtId = uniProtIds.get(v);
				entry.getUniProtIds().add(uniProtId);
				entry.getPdbIds().add(mapping.uniProtToPdb(uniProtId));
				entry.getScopIds().add(mapping.uniProtToScop(uniProtId));
			}
			entries.add(entry);
		}
		if (!entries.isEmpty()) {
			ReportGenerator.getInstance().put("degenerate_sets", entries);
		}
	}

	private void putUpdates(List<InteractionUpdate> updates) {
		NumberFormat nf = new DecimalFormat(); // we do it this way because our code knows better, weirdly
		nf.setMaximumFractionDigits(3);
		final IdentifierMapping mapping = IdentifierMappingFactory.getMapping();
		List<UpdateTableEntry> updated = new ArrayList<>();
		for (InteractionUpdate update : updates) {
			if (update.isRemoved()) continue;
			UpdateTableEntry entry1 = new UpdateTableEntry();
			entry1.before = nf.format(update.getInitialProbability());
			entry1.after = nf.format(update.getEdge().getWeight());
			entry1.id = update.getIds().getFirst();
			entry1.uniProtId = update.getUniProtIds().getFirst();
			entry1.pdbId = mapping.uniProtToPdb(entry1.uniProtId);
			entry1.pdbId = entry1.pdbId.substring(0, entry1.pdbId.length()-2);
			entry1.scopId = mapping.uniProtToScop(entry1.uniProtId);
			updated.add(entry1);
			UpdateTableEntry entry2 = new UpdateTableEntry();
			entry2.before = nf.format(update.getInitialProbability());
			entry2.after = nf.format(update.getEdge().getWeight());
			entry2.id = update.getIds().getSecond();
			entry2.uniProtId = update.getUniProtIds().getSecond();
			entry2.pdbId = mapping.uniProtToPdb(entry2.uniProtId);
			entry2.pdbId = entry2.pdbId.substring(0, entry2.pdbId.length()-2);
			entry2.scopId = mapping.uniProtToScop(entry2.uniProtId);
			updated.add(entry2);
		}
		if (!updated.isEmpty()) {
			ReportGenerator.getInstance().put("updated", updated);
		}
	}

	public void setBeta(double beta) {
		this.beta = beta;
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
