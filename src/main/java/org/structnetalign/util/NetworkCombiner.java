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

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.Participant;

public class NetworkCombiner {

	static final Logger logger = Logger.getLogger(NetworkCombiner.class.getName());

	private static Random random = new Random();

	private double probability = 0.05;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: NetworkCombiner output-file probability input-files...");
			return;
		}
		final File output = new File(args[0]);
		final double probability = Double.parseDouble(args[1]);
		File[] inputs = new File[args.length - 2];
		for (int i = 2; i < args.length; i++) {
			inputs[i - 2] = new File(args[i]);
		}
		NetworkCombiner combiner = new NetworkCombiner();
		combiner.setProbability(probability);
		combiner.combine(output, inputs);
	}

	public void combine(File output, File... inputs) {

		EntrySet myEntrySet = new EntrySet();

		for (int i = 0; i < inputs.length; i++) {

			EntrySet entrySet = NetworkUtils.readNetwork(inputs[i]);

			// do it this way so we don't have to read the first network twice
			if (i == 0) {
				myEntrySet.setVersion(entrySet.getVersion());
				myEntrySet.setMinorVersion(entrySet.getMinorVersion());
				myEntrySet.setLevel(entrySet.getLevel());
			} else {
				if (entrySet.getVersion() != myEntrySet.getVersion()) throw new IllegalArgumentException(
						"Different major version numbers!");
				if (entrySet.getVersion() != myEntrySet.getVersion()) throw new IllegalArgumentException(
						"Different minor version numbers!");
				if (entrySet.getLevel() != myEntrySet.getLevel()) throw new IllegalArgumentException(
						"Different level numbers!");
			}

			for (Entry entry : entrySet.getEntries()) {
				Entry myEntry = includeVertices(entry, probability);
				myEntrySet.getEntries().add(myEntry);
			}

			entrySet = null;
			System.gc(); // predictable GC times

		}

		NetworkUtils.writeNetwork(myEntrySet, output);
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	private Entry includeVertices(Entry entry, double probability) {

		Entry myEntry = new Entry();
		myEntry.setSource(entry.getSource());
		myEntry.getAttributes().addAll(entry.getAttributes());
		myEntry.getAvailabilities().addAll(entry.getAvailabilities());
		myEntry.getExperiments().addAll(entry.getExperiments());

		Set<Integer> set = new HashSet<Integer>();
		Collection<Interactor> interactors = entry.getInteractors();
		for (Interactor interactor : interactors) {
			final double r = random.nextDouble();
			if (r >= probability) {
				set.add(interactor.getId());
				myEntry.getInteractors().add(interactor);
			}
		}

		// now add the edges
		interactions: for (Interaction interaction : entry.getInteractions()) {

			Collection<Participant> participants = interaction.getParticipants();

			for (Participant participant : participants) {
				final int id = participant.getInteractor().getId();
				if (!set.contains(id)) continue interactions;
			}

			myEntry.getInteractions().add(interaction);

		}

		return myEntry;

	}

}
