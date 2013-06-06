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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.align.StructureAlignment;
import org.biojava.bio.structure.align.ce.CeMain;
import org.biojava.bio.structure.align.ce.CeParameters;
import org.biojava.bio.structure.align.ce.ConfigStrucAligParams;
import org.biojava.bio.structure.align.model.AFPChain;
import org.biojava.bio.structure.align.util.AFPChainScorer;
import org.biojava.bio.structure.align.util.AtomCache;
import org.structnetalign.util.IdentifierMappingFactory;

/**
 * A {@link Weight} that uses the <a href="http://www.ncbi.nlm.nih.gov/pubmed/9796821">Combinatorial Extension</a>
 * structural alignment method of Shindyalov and Bourne.
 * 
 * @author dmyersturnbull
 */
public class CeWeight implements AlignmentWeight {

	/**
	 * A factory that instantiates a new StructureAlignment for each new alignment. Useful for concurrency: use
	 * precisely 1 StructureAlignment object per concurrent alignment.
	 */
	public static abstract class AlgorithmGiver {
		public static AlgorithmGiver getDefault() {
			return new AlgorithmGiver() {
				@Override
				public StructureAlignment getAlgorithm() {
					CeMain ceSymm = new CeMain();
					ConfigStrucAligParams params = ceSymm.getParameters();
					if (params instanceof CeParameters) {
						CeParameters ceparams = (CeParameters) params;
						ceparams.setScoringStrategy(CeParameters.SEQUENCE_CONSERVATION);
						if (SEQUENCE_WEIGHT != null) ceparams.setSeqWeight(SEQUENCE_WEIGHT); // note the use of sequence
																								// weight
						if (GAP_OPEN != null) ceparams.setGapOpen(GAP_OPEN);
						if (GAP_EXTEND != null) ceparams.setGapExtension(GAP_EXTEND);
						if (MAX_GAP_SIZE != null) ceparams.setMaxGapSize(MAX_GAP_SIZE);
						ceSymm.setParameters(ceparams);
					}
					return ceSymm;
				}
			};
		}

		public abstract StructureAlignment getAlgorithm();
	}

	private static Double GAP_EXTEND;
	private static Double GAP_OPEN;
	private static Integer MAX_GAP_SIZE;

	private static Double SEQUENCE_WEIGHT = 2.0;

	private AlgorithmGiver algorithm;

	private String pdbIdAndChain1;

	private String pdbIdAndChain2;
	private String uniProtId1;

	private String uniProtId2;
	private int v1;

	private int v2;

	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("weight/ce_weights.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open databases property file", e);
		}
		String sequenceWeight = props.getProperty("sequence_weight");
		if (sequenceWeight != null) {
			SEQUENCE_WEIGHT = Double.parseDouble(sequenceWeight);
		}
		String gapOpen = props.getProperty("gap_open");
		if (gapOpen != null) {
			GAP_OPEN = Double.parseDouble(gapOpen);
		}
		String gapExtend = props.getProperty("gap_extend");
		if (gapExtend != null) {
			GAP_EXTEND = Double.parseDouble(gapExtend);
		}
		String maxGapSize = props.getProperty("max_gap_size");
		if (maxGapSize != null) {
			MAX_GAP_SIZE = Integer.parseInt(maxGapSize);
		}
	}

	/**
	 * Disable on production.
	 * 
	 * @param ca1
	 * @param ca2
	 * @return
	 */
	private static boolean sanityCheckPreAlign(Atom[] ca1, Atom[] ca2) {
		if (ca1 == ca2) return false;
		if (ca1[0].getGroup().getChain().getParent() == ca2[0].getGroup().getChain().getParent()) return false;
		return true;
	}

	public CeWeight() {
		this(AlgorithmGiver.getDefault());
	}

	public CeWeight(final AlgorithmGiver algorithm) {
		this.algorithm = algorithm;
	}

	@Override
	public double assignWeight(int v1, int v2, String uniProtId1, String uniProtId2) throws Exception {
		setIds(v1, v2, uniProtId1, uniProtId2);
		return call().getWeight();
	}

	@Override
	public WeightResult call() throws Exception {
		final AtomCache cache = AtomCacheFactory.getCache();
		Atom[] ca1, ca2;
		try {
			ca1 = cache.getAtoms(pdbIdAndChain1);
		} catch (IOException | StructureException e) {
			throw new WeightException("Could not parse structure for PDB entry " + pdbIdAndChain1 + " for "
					+ uniProtId1, e, uniProtId1, uniProtId2, true, true);
		}
		try {
			ca2 = cache.getAtoms(pdbIdAndChain2);
		} catch (IOException | StructureException e) {
			throw new WeightException("Could not parse structure for PDB entry " + pdbIdAndChain2 + " for "
					+ uniProtId2, e, uniProtId1, uniProtId2, true, true);
		}
		AFPChain afpChain;
		try {
			afpChain = align(ca1, ca2);
		} catch (IOException | StructureException e) {
			throw new WeightException("Could not align " + pdbIdAndChain1 + " against " + pdbIdAndChain2, e,
					uniProtId1, uniProtId2, true, true);
		}
		if (afpChain.getTMScore() == -1) throw new WeightException("TM-score not calculated for the alignment of "
				+ pdbIdAndChain1 + " against " + pdbIdAndChain2, uniProtId1, uniProtId2, true, true);
		return new WeightResult(afpChain.getTMScore(), v1, v2, uniProtId1, uniProtId2, this.getClass());
	}

	@Override
	public void setIds(int v1, int v2, String uniProtId1, String uniProtId2) throws WeightException {

		this.v1 = v1;
		this.v2 = v2;
		this.uniProtId1 = uniProtId1;
		this.uniProtId2 = uniProtId2;

		pdbIdAndChain1 = IdentifierMappingFactory.getMapping().uniProtToPdb(uniProtId1);
		if (pdbIdAndChain1 == null) throw new WeightException("Could not find PDB Id for " + uniProtId1, uniProtId1,
				uniProtId2, true, true);
		pdbIdAndChain2 = IdentifierMappingFactory.getMapping().uniProtToPdb(uniProtId2);
		if (pdbIdAndChain2 == null) throw new WeightException("Could not find PDB Id for " + uniProtId2, uniProtId1,
				uniProtId2, true, true);

	}

	private AFPChain align(Atom[] ca1, Atom[] ca2) throws StructureException, IOException {
		if (!sanityCheckPreAlign(ca1, ca2)) throw new IllegalArgumentException("Can't align using same structure.");
		AFPChain afpChain = algorithm.getAlgorithm().align(ca1, ca2);
		if (afpChain == null) return null;
		afpChain.setName1(pdbIdAndChain1);
		afpChain.setName2(pdbIdAndChain2);
		double realTmScore = AFPChainScorer.getTMScore(afpChain, ca1, ca2);
		afpChain.setTMScore(realTmScore);
		return afpChain;
	}

}
