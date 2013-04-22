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
package org.structnetalign.weight;

import java.io.IOException;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.align.StructureAlignment;
import org.biojava.bio.structure.align.ce.CeMain;
import org.biojava.bio.structure.align.ce.CeParameters;
import org.biojava.bio.structure.align.ce.ConfigStrucAligParams;
import org.biojava.bio.structure.align.model.AFPChain;
import org.biojava.bio.structure.align.util.AFPChainScorer;
import org.biojava.bio.structure.align.util.AtomCache;
import org.structnetalign.util.IdentifierMapping;

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
						ceparams.setSeqWeight(2); // note the use of sequence weight
						ceparams.setScoringStrategy(CeParameters.SIDE_CHAIN_SCORING);
						ceSymm.setParameters(ceparams);
					}
					return ceSymm;
				}
			};
		}

		public abstract StructureAlignment getAlgorithm();
	}

	private AlgorithmGiver algorithm;

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
	public double assignWeight(String uniProtId1, String uniProtId2) throws WeightException {
		final String scopId1 = IdentifierMapping.uniProtToScop(uniProtId1);
		if (scopId1 == null) throw new WeightException("Could not find SCOP id for " + uniProtId1);
		final String scopId2 = IdentifierMapping.uniProtToScop(uniProtId2);
		if (scopId2 == null) throw new WeightException("Could not find SCOP id for " + uniProtId2);
		AFPChain afpChain;
		try {
			afpChain = align(scopId1, scopId2);
		} catch (IOException | StructureException e) {
			throw new WeightException("Could not align " + scopId1 + " against " + scopId2, e);
		}
		if (afpChain.getTMScore() == -1) throw new WeightException("TM-score not calculated for the alignment of "
				+ scopId1 + " against " + scopId2);
		return afpChain.getTMScore();
	}

	private AFPChain align(String name1, String name2) throws IOException, StructureException {
		final AtomCache cache = AtomCacheFactory.getCache();
		Atom[] ca1 = cache.getAtoms(name1); // C-alpha only
		Atom[] ca2 = cache.getAtoms(name2);
		AFPChain afpChain = align(name1, name2, ca1, ca2);
		return afpChain;
	}

	private AFPChain align(String name1, String name2, Atom[] ca1, Atom[] ca2) throws StructureException, IOException {
		if (!sanityCheckPreAlign(ca1, ca2)) throw new IllegalArgumentException("Can't align using same structure.");
		AFPChain afpChain = algorithm.getAlgorithm().align(ca1, ca2);
		if (afpChain == null) return null;
		afpChain.setName1(name1);
		afpChain.setName2(name2);
		double realTmScore = AFPChainScorer.getTMScore(afpChain, ca1, ca2);
		afpChain.setTMScore(realTmScore);
		return afpChain;
	}

}
