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
package org.structnetalign.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.structure.align.ce.AbstractUserArgumentProcessor;
import org.structnetalign.PipelineManager;
import org.structnetalign.PipelineProperties;
import org.structnetalign.util.NetworkPreparer;
import org.structnetalign.util.NetworkUtils;
import org.structnetalign.weight.AtomCacheFactory;
import org.structnetalign.weight.SimpleWeightCreator;
import org.structnetalign.weight.WeightCreator;

import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * A benchmark that compares input and output networks to a data set from Interactome3D.
 * @author dmyersturnbull
 *
 */
public class Interactome3DComparator {

	private static enum InteractomePresence {
		MODEL, STRUCTURAL;
		static InteractomePresence parse(String s) {
			if (s == null) return null;
			if (s.equals("Structure")) return STRUCTURAL;
			if (s.equals("Model") || s.equals("Dom_dom_model")) return MODEL;
			return null;
		}
	}

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private static final double MISSING_SCORE = -0.2;

	private static final double MODEL_SCORE = 0.5;

	private static final double STRUCTURAL_SCORE = 1.0;
	
	private transient HashMap<Pair<String>, InteractomePresence> benchmark;
	private final File inputFile;

	private final File interactome3DFile;
	private final File outputFile;
	private final WeightCreator phi;
	private final File preparedFile;

	private final double tau;
	private final int xi;
	private final double zeta;

	public static void benchmark(File inputFile, File interactome3DFile) throws IOException {
		Interactome3DComparator comp = new Interactome3DComparator(inputFile, interactome3DFile);
		double a = comp.scoreInput();
		double b = comp.scorePrepared();
		double c = comp.scoreOutput();
		NumberFormat nf = PipelineProperties.getInstance().getOutputFormatter();
		System.out.println(nf.format(a));
		System.out.println(nf.format(b));
		System.out.println(nf.format(c));
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println("Usage: " + Interactome3DComparator.class.getSimpleName()
					+ " input-file interactome3d-interactions-file pdb-dir");
			return;
		}
		File inputFile = new File(args[0]);
		File interactome3DFile = new File(args[1]);
		benchmark(inputFile, interactome3DFile);
		System.setProperty(AbstractUserArgumentProcessor.PDB_DIR, args[2]);
		AtomCacheFactory.setCache(args[2]);
	}

	public Interactome3DComparator(File inputFile, File interactome3DFile) throws IOException {
		this(inputFile, interactome3DFile, new SimpleWeightCreator(), 1, 1,
				PipelineManager.XI);
	}

	public Interactome3DComparator(File inputFile, File interactome3DFile, SimpleWeightCreator phi, double tau,
			double zeta, int xi) throws IOException {
		this.phi = phi;
		this.tau = tau;
		this.zeta = zeta;
		this.xi = xi;
		this.inputFile = inputFile;
		String dir = inputFile.getParent() + File.separator + "bench3d_data";
		new File(dir).mkdir();
		preparedFile = new File(dir + File.separator + "prepared.mif25.tmp");
		outputFile = new File(dir + File.separator + "output.mif25.tmp");
		this.interactome3DFile = interactome3DFile;
		init();
	}

	public double scoreInput() {
		return score(inputFile, "", "", 1.0);
	}

	public double scoreOutput() {
		PipelineManager manager = new PipelineManager();
		manager.setPhi(phi);
		manager.setNoMerge(true);
		manager.setTau(tau);
		manager.setZeta(zeta);
		manager.setXi(xi);
		manager.run(preparedFile, outputFile);
		return score(outputFile, PipelineProperties.getInstance().getOutputConfLabel(), PipelineProperties
				.getInstance().getOutputConfName(), null);
	}

	public double scorePrepared() {
		NetworkPreparer prep = new NetworkPreparer();
		prep.prepare(inputFile, preparedFile);
		return score(preparedFile, PipelineProperties.getInstance().getInitialConfLabel(), PipelineProperties
				.getInstance().getInitialConfLabel(), null);
	}

	private void init() throws IOException {
		benchmark = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(interactome3DFile))) {
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");
				// PROT1 PROT2 RANK_MAJOR RANK_MINOR TYPE PDB_ID BIO_UNIT CHAIN1 MODEL1 SEQ_IDENT1 COVERAGE1 SEQ_BEGIN1
				// SEQ_END1 DOMAIN1 CHAIN2 MODEL2 SEQ_IDENT2 COVERAGE2 SEQ_BEGIN2 SEQ_END2 DOMAIN2 FILENAME
				Pair<String> pair = new Pair<>(parts[0], parts[1]);
				InteractomePresence p = InteractomePresence.parse(parts[4]);
				benchmark.put(pair, p);
				logger.debug("Putting (" + pair + ", " + parts[4] + ")");
			}
		}
	}

	private double score(File file, String confLabel, String confName, Double fallback) {

		double score = 0;
		double norm = 0;

		EntrySet entrySet = NetworkUtils.readNetwork(file);

		for (Entry entry : entrySet.getEntries()) {

			for (Interaction interaction : entry.getInteractions()) {
				Pair<String> pair;
				try {
					pair = NetworkUtils.getUniProtId(interaction);
				} catch (IllegalArgumentException e) {
					logger.debug("Not exactly 2 participants for " + interaction.getId(), e);
					continue;
				}
				Double removed = NetworkUtils.getExistingAnnotationValue(interaction, PipelineProperties.getInstance()
						.getRemovedAttributeLabel());
				if (removed != null) {
					logger.debug("Skipping " + pair + " because it was removed by StructNA");
					continue;
				}
				Double value = NetworkUtils.getExistingConfidenceValue(interaction, confLabel, confName);
				if (value == null) {
					if (fallback != null) {
						value = fallback;
					} else {
						logger.warn("Could not get value for " + pair);
						continue;
					}
				}
				InteractomePresence presence = benchmark.get(pair);
				logger.debug(pair + " has presence " + presence);
				double coeff = 0;
				if (presence == null) {
					coeff = MISSING_SCORE;
				} else if (presence == InteractomePresence.STRUCTURAL) {
					coeff = STRUCTURAL_SCORE;
				} else if (presence == InteractomePresence.MODEL) {
					coeff = MODEL_SCORE;
				} else {
					throw new RuntimeException("This really shouldn't happen");
				}
				if (value != null) {
					score += coeff * Math.exp(value);
					norm += Math.exp(value);
				}
			}
		}
		logger.info("Got score of " + score + " and norm of " + norm);
		return score / norm;
	}
}
