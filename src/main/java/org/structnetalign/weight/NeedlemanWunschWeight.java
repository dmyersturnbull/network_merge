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
import java.net.URL;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	private static final Logger logger = LogManager.getLogger(NeedlemanWunschWeight.class.getName());
	
	private static GammaScorer GAMMA = GammaScorer.forBlosum62();

	private static GapPenalty GAP_PENALTY = new SimpleGapPenalty((short) 12, (short) 1);

	private static SubstitutionMatrix<AminoAcidCompound> MATRIX = SubstitutionMatrixHelper.getBlosum62();

	private static String URL;

	private String uniProtId1;

	private String uniProtId2;

	int nSamples;

	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("weight/nw_weights.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open needleman-wunsch property file", e);
		}
		String matrix = props.getProperty("matrix");
		if (matrix != null) {
			MATRIX = SubstitutionMatrixHelper.getMatrixFromAAINDEX(matrix);
			if (MATRIX == null) throw new IllegalArgumentException("Matrix " + matrix + " was not found");
		}
		String gapOpen = props.getProperty("gap_open");
		String gapExtend = props.getProperty("gap_extend");
		if (gapOpen != null || gapExtend != null) {
			GAP_PENALTY = new SimpleGapPenalty(Short.parseShort(gapOpen), Short.parseShort(gapExtend));
		}
		String alpha = props.getProperty("gamma_shape");
		String beta = props.getProperty("gamma_scale");
		String lambda = props.getProperty("gamma_shift");
		if (alpha != null || beta != null || lambda != null) {
			GAMMA = new GammaScorer(Double.parseDouble(alpha), Double.parseDouble(beta), Double.parseDouble(lambda));
			logger.info("Setting new gamma distribution (" + alpha + "," + beta + "," + lambda + ")");
		}

	}

	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("databases.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open databases property file", e);
		}
		URL = props.getProperty("uniprot_url");
	}

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
		ProteinSequence a, b;
		try {
			a = getSequenceForId(uniProtId1);
		} catch (Exception e) {
			throw new WeightException("Could not get FASTA sequence for " + uniProtId1, uniProtId1, uniProtId2, true,
					false);
		}
		try {
			b = getSequenceForId(uniProtId2);
		} catch (Exception e) {
			throw new WeightException("Could not get FASTA sequence for " + uniProtId1, uniProtId1, uniProtId2, true,
					false);
		}
		NeedlemanWunsch<ProteinSequence, AminoAcidCompound> alg = new NeedlemanWunsch<>(a, b, GAP_PENALTY, MATRIX);
		alg.setQuery(a);
		alg.setTarget(b);
		SequencePair<ProteinSequence, AminoAcidCompound> pair = alg.getPair();
		PairwiseSequenceScorer<ProteinSequence, AminoAcidCompound> scorer = new FractionalIdentityScorer<>(pair);
		double score = (double) scorer.getScore() / (double) scorer.getMaxScore();
		double prob = GAMMA.score(pair, score);
		return new WeightResult(prob, uniProtId1, uniProtId2, this.getClass());
	}

	@Override
	public void setIds(String uniProtId1, String uniProtId2) throws WeightException {
		this.uniProtId1 = uniProtId1;
		this.uniProtId2 = uniProtId2;
	}
}
