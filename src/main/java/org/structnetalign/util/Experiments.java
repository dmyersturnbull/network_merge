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
package org.structnetalign.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.ExperimentDescription;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.InteractionDetectionMethod;
import psidev.psi.mi.xml.model.Names;

public class Experiments {

	private static Experiments instance;

	public static Experiments getInstance() {
		return instance;
	}
	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("experiments.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open databases property file", e);
		}
		instance = new Experiments();
		for (Map.Entry<Object,Object> entry : props.entrySet()) {
			String name = String.valueOf(entry.getKey());
			double value = Double.parseDouble(String.valueOf(entry.getValue()));
			if (name.equals("default")) {
				System.err.println("DEFAULT IS " + value);
				instance.defaultWeight = value;
			} else {
				instance.weights.put(name, value);
			}
		}
	}
	
	private Experiments() {
		weights = new HashMap<>();
	}

	private double defaultWeight;
	private Map<String,Double> weights;
	
	public Double getWeight(String name) {
		if (name == null) return defaultWeight;
		if (!weights.containsKey(name)) return defaultWeight;
		return weights.get(name);
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Usage: " + Experiments.class.getSimpleName() + " input-psimi-files ...");
			return;
		}
		SortedMap<String,Integer> names = listDetectionMethodNames(args);
		for (Map.Entry<String,Integer> entry : names.entrySet()) {
			System.out.println(entry.getKey().replaceAll("[\\s]+", "\\\\ ") + "=0.8");
		}
	}

	public static SortedMap<String,Integer> listDetectionMethodNames(String... files) {
		final Map<String,Integer> unsorted = new HashMap<>();
		for (String file : files) {
			EntrySet entrySet = NetworkUtils.readNetwork(file);
			unsorted.putAll(listDetectionMethodNames(entrySet));
		}
		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (o1.equals(o2)) return 0;
				int compared = unsorted.get(o2).compareTo(unsorted.get(o1));
				if (compared != 0) return compared;
				return -1; // only say they're the same if the strings are equal
			}
		};
		SortedMap<String,Integer> names = new TreeMap<>(comparator);
		names.putAll(unsorted);
		return names;
	}
	public static Map<String,Integer> listDetectionMethodNames(EntrySet entrySet) {
		final HashMap<String,Integer> names = new HashMap<>();
		for (Entry entry : entrySet.getEntries()) {
			for (Interaction interaction : entry.getInteractions()) { // for some reason entry.getExperiments() doesn't return everything
				Collection<ExperimentDescription> experiments = interaction.getExperiments();
				for (ExperimentDescription experiment : experiments) {
					InteractionDetectionMethod method = experiment.getInteractionDetectionMethod();
					if (method != null) {
						Names theNames = method.getNames();
						if (theNames != null) {
							String name = theNames.getFullName();
							if (!names.containsKey(name)) {
								names.put(name, 0);
							}
							names.put(name, names.get(name) + 1);
						}
					}
				}
			}
		}
		return names;
	}

}
