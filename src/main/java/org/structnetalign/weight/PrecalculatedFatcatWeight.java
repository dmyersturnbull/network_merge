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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.align.model.AFPChain;
import org.biojava.bio.structure.align.util.AFPChainScorer;
import org.biojava.bio.structure.align.util.AtomCache;
import org.biojava.bio.structure.align.xml.AFPChainXMLParser;
import org.structnetalign.util.IdentifierMappingFactory;

/**
 * A simple {@link AlignmentWeight} that downloads pre-calculated FATCAT structural alignments from <a
 * href="http://rcsb.org">RCSB</a> and uses the TM-score of the alignment as the weight. Unfortunately it needs to load the PDB structure,
 * download the entire AFPChain XML representation, and re-apply the alignment transformation, so it isn't super-fast.
 *
 * @author dmyersturnbull
 * 
 */
public class PrecalculatedFatcatWeight implements AlignmentWeight {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private static final int DEFAULT_TIMEOUT = 5*1000;
	private static String BASE_URL;
	private static String PARAM;
	private static int TIMEOUT = DEFAULT_TIMEOUT;

	private String pdbIdAndChain1;

	private String pdbIdAndChain2;
	private String uniProtId1;

	private String uniProtId2;
	private int v1;

	private int v2;

	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("databases.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open databases property file", e);
		}
		BASE_URL = props.getProperty("precalc_fatcat_url");
		PARAM = props.getProperty("precalc_fatcat_struct_param");
	}
	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("weight/precalc_fatcat_weights.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open precalculated FATCAT weights property file", e);
		}
		String timeout = props.getProperty("timeout");
		if (timeout != null) {
			TIMEOUT = Integer.parseInt(timeout);
			logger.info("Setting precalculated FATCAT timeout to " + timeout);
		}
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
					+ uniProtId1, e, v1, v2, uniProtId1, uniProtId2, true, true);
		}
		try {
			ca2 = cache.getAtoms(pdbIdAndChain2);
		} catch (IOException | StructureException e) {
			throw new WeightException("Could not parse structure for PDB entry " + pdbIdAndChain2 + " for "
					+ uniProtId2, e, v1, v2, uniProtId1, uniProtId2, true, true);
		}
		AFPChain afpChain;
		try {
			afpChain = load(pdbIdAndChain1, pdbIdAndChain2, ca1, ca2);
		} catch (MalformedURLException e) {
			throw new WeightException("Could not create URL for " + pdbIdAndChain2 + " for " + uniProtId2, e, v1, v2,
					uniProtId1, uniProtId2, true, true);
		} catch (IOException e) {
			throw new WeightException("Could not read stream for " + pdbIdAndChain2 + " for " + uniProtId2, e, v1, v2,
					uniProtId1, uniProtId2, true, true);
		} catch (StructureException e) {
			throw new WeightException("Could not get complete AFPChain for " + pdbIdAndChain2 + " for " + uniProtId2,
					e, v1, v2, uniProtId1, uniProtId2, true, true);
		}
		if (afpChain.getTMScore() == -1) throw new WeightException("TM-score not calculated for the alignment of "
				+ pdbIdAndChain1 + " against " + pdbIdAndChain2, v1, v2, uniProtId1, uniProtId2, true, true);
		return new WeightResult(afpChain.getTMScore(), v1, v2, uniProtId1, uniProtId2, this.getClass());
	}

	@Override
	public void setIds(int v1, int v2, String uniProtId1, String uniProtId2) throws WeightException {

		this.v1 = v1;
		this.v2 = v2;
		this.uniProtId1 = uniProtId1;
		this.uniProtId2 = uniProtId2;

		pdbIdAndChain1 = IdentifierMappingFactory.getMapping().uniProtToPdb(uniProtId1);
		if (pdbIdAndChain1 == null) throw new WeightException("Could not find PDB Id for " + uniProtId1, v1, v2,
				uniProtId1, uniProtId2, true, true);
		pdbIdAndChain2 = IdentifierMappingFactory.getMapping().uniProtToPdb(uniProtId2);
		if (pdbIdAndChain2 == null) throw new WeightException("Could not find PDB Id for " + uniProtId2, v1, v2,
				uniProtId1, uniProtId2, true, true);

	}

	private AFPChain load(String id1, String id2, Atom[] ca1, Atom[] ca2) throws IOException, StructureException {
		URL url = new URL(BASE_URL + "&" + PARAM + "=" + id1 + "&" + PARAM + "=" + id2);
		URLConnection conn = url.openConnection();
		conn.setReadTimeout(TIMEOUT);
		logger.debug("Loading AFPChain from URL " + url);
		String string;
		try (InputStream is = conn.getInputStream()) {
			string = IOUtils.toString(is, "UTF-8"); // thanks Apache Commons!
		}
		AFPChain afpChain = AFPChainXMLParser.fromXML(string, ca1, ca2);
		// now we need to rotate to make the structure match the alignment
		double tmScore = AFPChainScorer.getTMScore(afpChain, ca1, ca2);
		afpChain.setTMScore(tmScore);
		return afpChain;
	}

}
