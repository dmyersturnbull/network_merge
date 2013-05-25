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

import java.io.InputStream;
import java.net.URL;

import org.biojava3.alignment.FractionalIdentityScorer;
import org.biojava3.alignment.NeedlemanWunsch;
import org.biojava3.alignment.SimpleGapPenalty;
import org.biojava3.alignment.SubstitutionMatrixHelper;
import org.biojava3.alignment.template.GapPenalty;
import org.biojava3.alignment.template.PairwiseSequenceScorer;
import org.biojava3.alignment.template.SequencePair;
import org.biojava3.alignment.template.SubstitutionMatrix;
import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.compound.AminoAcidCompound;
import org.biojava3.core.sequence.io.FastaReaderHelper;

public class NeedlemanWunschWeight implements AlignmentWeight {

	private static class PoissonScorer {
		private static final double LN2 = Math.log(2);
		private final double k;
		private final double lambda;

		public PoissonScorer(double lambda, double k) {
			super();
			this.lambda = lambda;
			this.k = k;
		}

		public double score(SequencePair<ProteinSequence, AminoAcidCompound> pair, double score) {
			double bitScore = (lambda * score - Math.log(k)) / LN2;
			double eValue = pair.getSize() * pair.getLength() * Math.pow(2, -2 * bitScore);
			return Math.exp(-eValue);
		}
	}

	private static final GapPenalty GAP_PENALTY = new SimpleGapPenalty();

	/**
	 * This value comes from NCBI BLAST somewhere. Will find. Note that it is not perfect for this project, but it's
	 * good enough for now.
	 */
	private static final double K = 0.711;

	/**
	 * This value comes from NCBI BLAST somewhere. Will find. Note that it is not perfect for this project, but it's
	 * good enough for now.
	 */
	private static final double LAMBDA = 1.37;

	private static final SubstitutionMatrix<AminoAcidCompound> MATRIX = SubstitutionMatrixHelper.getBlosum62();

	private static final String URL = "http://www.uniprot.org/uniprot/%s.fasta";

	private String uniProtId1;

	private String uniProtId2;

	private static ProteinSequence getSequenceForId(String uniProtId) throws Exception {
		try (InputStream stream = new URL(String.format(URL, uniProtId)).openStream()) {
			return FastaReaderHelper.readFastaProteinSequence(stream).get(uniProtId); // why does this throw Exception?
		}
	}

	@Override
	public double assignWeight(String uniProtId1, String uniProtId2) throws Exception {
		setIds(uniProtId1, uniProtId2);
		return call().getWeight();
	}

	@Override
	public WeightResult call() throws Exception {
		ProteinSequence a = getSequenceForId(uniProtId1);
		ProteinSequence b = getSequenceForId(uniProtId2);
		NeedlemanWunsch<ProteinSequence, AminoAcidCompound> alg = new NeedlemanWunsch<>(a, b, GAP_PENALTY, MATRIX);
		SequencePair<ProteinSequence, AminoAcidCompound> pair = alg.getPair();
		PairwiseSequenceScorer<ProteinSequence, AminoAcidCompound> scorer = new FractionalIdentityScorer<>(pair);
		double score = scorer.getScore() / scorer.getMaxScore();
		PoissonScorer poisson = new PoissonScorer(LAMBDA, K);
		double prob = poisson.score(pair, score);
		return new WeightResult(prob, uniProtId1, uniProtId2);
	}

	@Override
	public void setIds(String uniProtId1, String uniProtId2) throws WeightException {
		this.uniProtId1 = uniProtId1;
		this.uniProtId2 = uniProtId2;
	}
}
