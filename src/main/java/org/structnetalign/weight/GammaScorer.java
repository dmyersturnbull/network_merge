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

import org.apache.commons.math3.distribution.GammaDistribution;
import org.biojava3.alignment.template.SequencePair;
import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.compound.AminoAcidCompound;


/**
 * Scores with a gamma distribution as per Webber and Barton 2001, Bioinformatics. The authors used (among other
 * scoring schemes) the BLOSUM62 matrix with a gap opening penalty of 12 and extension penalty of 1. The paper is <a
 * href="http://bioinformatics.oxfordjournals.org/content/17/12/1158.full.pdf+html">available</a>.
 * 
 * @author dmyersturnbull
 * 
 */
public class GammaScorer {
	private double alpha; // shape (normally theta)
	private double beta; // scale
	private double lambda;

	public static GammaScorer forBlosum62() {
		return new GammaScorer(25.54, 4.96, 0.2);
	}

	public GammaScorer(double alpha, double beta, double lambda) {
		super();
		this.alpha = alpha;
		this.beta = beta;
		this.lambda = lambda;
	}

	public double score(SequencePair<ProteinSequence, AminoAcidCompound> pair, double score) {
		GammaDistribution dist = new GammaDistribution(alpha, lambda);
		return dist.density(score + beta);
	}

}
