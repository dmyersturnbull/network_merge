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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.structure.scop.ScopCategory;
import org.biojava.bio.structure.scop.ScopDatabase;
import org.biojava.bio.structure.scop.ScopDescription;
import org.biojava.bio.structure.scop.ScopDomain;
import org.biojava.bio.structure.scop.ScopFactory;
import org.biojava.bio.structure.scop.ScopNode;
import org.structnetalign.util.BasicScop;

/**
 * Determines how common each SCOP relation (e.g. superfamily) is.
 * 
 * @author dmyersturnbull
 * 
 */
public class ScopPrevalence {

	private static final Logger logger = LogManager.getLogger(ScopPrevalence.class.getName());

	private Map<ScopCategory, Double> freqs = new TreeMap<>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ScopPrevalence prevalence = new ScopPrevalence(1000);
		for (Map.Entry<ScopCategory, Double> entry : prevalence.getFreqs().entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
	}

	/**
	 * A recursive method that fills {@code descs} with a list of {@link ScopDescription ScopDescriptions} of the
	 * specified {@link ScopCategory} that are underneath {@code sunId} in the SCOP tree.
	 * 
	 * @param sunId
	 * @param category
	 * @param descs
	 */
	private static void getUnder(int sunId, ScopCategory category, List<ScopDescription> descs) {

		final ScopDatabase scop = ScopFactory.getSCOP();
		final ScopDescription description = scop.getScopDescriptionBySunid(sunId);

		if (description.getCategory().equals(category)) { // base case
			descs.add(scop.getScopDescriptionBySunid(sunId));
		} else { // recurse
			final ScopNode node = scop.getScopNode(sunId);
			for (int s : node.getChildren()) {
				getUnder(s, category, descs);
			}
		}
	}

	private static int sunIdOfCategory(ScopDomain domain, ScopCategory category) {
		switch (category) {
		case Class:
			return domain.getClassId();
		case Fold:
			return domain.getFoldId();
		case Superfamily:
			return domain.getSuperfamilyId();
		case Family:
			return domain.getFamilyId();
		case Domain:
			return domain.getDomainId();
		case Px:
			return domain.getPx();
		case Species:
			return domain.getSpeciesId();
		default:
			throw new IllegalArgumentException("Invalid SCOP category " + category.name());
		}
	}

	/**
	 * Creates a new ScopPrevalence using the specified sample size {@code number}. Uses SCOP classes a, b, c, d, e, and
	 * f. Does not normalize the numbers in any way, so the numbers will be over all domains in SCOP.
	 * 
	 * @param number
	 *            The number of SCOP random domains to use; there will be {@code number} squared relations examined
	 */
	public ScopPrevalence(int number) {
		this(number, new Random(), ScopCategory.Domain, Arrays.asList(new Integer[] { 46456, 48724, 51349, 53931,
				56572, 56835 }));
	}

	/**
	 * Creates a new ScopPrevalence.
	 * 
	 * @param number
	 *            The number of SCOP random domains to use; there will be {@code number} squared relations examined
	 * @param random
	 *            The random-number generator to use
	 * @param normalization
	 *            How to normalize the relation statistics; for example if set to ScopCategory.family, will report
	 *            statistics that are effectively <em>per-family</em>, or normalized by the number of domains per family
	 * @param classSunIds
	 *            The Sun Ids of the SCOP classes to consider
	 */
	public ScopPrevalence(int number, Random random, ScopCategory normalization, List<Integer> classSunIds) {
		ScopDomain[] domains = buildDomainList(number, random, normalization, classSunIds);
		Map<ScopCategory, Integer> counts = new TreeMap<>();
		for (int i = 0; i < number; i++) {
			for (int j = 0; j < number; j++) {
				// we need to iterate in reverse order (most specific first)
				ScopCategory[] categories = ScopCategory.values();
				Collections.reverse(Arrays.asList(categories));
				for (ScopCategory category : categories) {
					int categoryId1 = sunIdOfCategory(domains[i], category);
					int categoryId2 = sunIdOfCategory(domains[j], category);
					if (categoryId1 == categoryId2) {
						if (!counts.containsKey(category)) counts.put(category, 0);
						counts.put(category, counts.get(category) + 1);
						break;
					}
				}
			}
		}
		double max = 0;
		for (Map.Entry<ScopCategory, Integer> entry : counts.entrySet()) {
			double x = (double) (number * number) / entry.getValue();
			freqs.put(entry.getKey(), x);
			if (x > max) max = x;
		}
		for (Map.Entry<ScopCategory, Double> entry : freqs.entrySet()) {
			entry.setValue(entry.getValue() / max);
		}
	}

	public Map<ScopCategory, Double> getFreqs() {
		return freqs;
	}

	private ScopDomain[] buildDomainList(int number, Random random, ScopCategory category, List<Integer> classSunIds) {
		ScopDatabase scop = BasicScop.getScop();
		List<ScopDescription> allOfCategory = new ArrayList<ScopDescription>();
		for (int classSunId : classSunIds) {
			getUnder(classSunId, category, allOfCategory);
		}
		logger.info("Found " + allOfCategory.size() + " categories in " + classSunIds.size() + " classes");
		ScopDomain[] domains = new ScopDomain[number];
		for (int i = 0; i < number; i++) {
			logger.debug("Working on " + i);
			int categoryChoice = random.nextInt(allOfCategory.size());
			ScopDescription chosenCategory = allOfCategory.get(categoryChoice);
			logger.debug("Chose " + category.name() + " " + chosenCategory.getClassificationId() + " from "
					+ allOfCategory.size() + " choices");
			List<ScopDescription> matchingDomainDescriptions = new ArrayList<ScopDescription>();
			getUnder(chosenCategory.getSunID(), ScopCategory.Domain, matchingDomainDescriptions);
			int domainDescriptionChoice = random.nextInt(matchingDomainDescriptions.size());
			ScopDescription chosenDescription = matchingDomainDescriptions.get(domainDescriptionChoice);
			logger.debug("Chose domain description " + chosenDescription.getClassificationId() + " from "
					+ matchingDomainDescriptions.size() + " choices");
			List<ScopDomain> matchingDomains = scop.getScopDomainsBySunid(chosenDescription.getSunID());
			int domainChoice = random.nextInt(matchingDomains.size());
			ScopDomain chosenDomain = matchingDomains.get(domainChoice);
			logger.debug("Chose domain " + chosenDomain.getScopId() + " from " + matchingDomains.size() + " choices");
			domains[i] = chosenDomain;
		}
		return domains;
	}

}
