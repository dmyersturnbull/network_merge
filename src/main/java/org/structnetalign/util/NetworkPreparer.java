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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import org.structnetalign.InteractionEdge;

import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.Participant;
import psidev.psi.mi.xml.model.Source;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.UndirectedGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
 
public class NetworkPreparer {

	static final Logger logger = LogManager.getLogger("org.structnetalign");

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: NetworkPreparer input-file output-dir");
			return;
		}
		NetworkPreparer preparer = new NetworkPreparer();
		preparer.prepare(new File(args[0]), args[1]);
	}

	public List<EntrySet> getConnnectedComponents(EntrySet entrySet) {

		// first, find the connected components as sets of vertices
		UndirectedGraph<Integer, InteractionEdge> graph = GraphInteractionAdaptor.toGraph(entrySet, 1);
		logger.info("There are " + graph.getVertexCount() + " vertices and " + graph.getEdgeCount() + " edges");
		WeakComponentClusterer<Integer, InteractionEdge> alg = new WeakComponentClusterer<>();
		Set<Set<Integer>> ccs = alg.transform(graph);
		logger.info("Found " + ccs.size() + " connected components");

		// map each vertex id to an interactor and a set of interactions
		HashMap<Integer, Interactor> interactorMap = new HashMap<>();
		HashMap<Integer, Set<Interaction>> interactionMap = new HashMap<>();
		for (Entry entry : entrySet.getEntries()) {
			for (Interactor interactor : entry.getInteractors()) {
				interactorMap.put(interactor.getId(), interactor);
				interactionMap.put(interactor.getId(), new HashSet<Interaction>());
			}
			for (Interaction interaction : entry.getInteractions()) {
				NavigableSet<Integer> participants = NetworkUtils.getVertexIds(interaction);
				for (int participant : participants) {
					Set<Interaction> interactionsOfInteractor = interactionMap.get(participant);
					interactionsOfInteractor.add(interaction);
				}
			}
		}

		// the schema requires a non-null source
		Source source = null;
		for (Entry entry : entrySet.getEntries()) {
			if (entry.getSource() != null) {
				source = entry.getSource();
				break;
			} else {
				logger.warn("An entry is missing a source");
			}
		}
		
		// now create an EntrySet per cc
		List<EntrySet> myEntrySets = new ArrayList<>(ccs.size());
		for (Set<Integer> cc : ccs) {
			EntrySet myEntrySet = NetworkUtils.skeletonClone(entrySet);
			Entry myEntry = new Entry();
			myEntry.setSource(source);
			for (int id : cc) {
				Interactor interactor = interactorMap.get(id);
				Set<Interaction> interactions = interactionMap.get(id);
				myEntry.getInteractors().add(interactor);
				myEntry.getInteractions().addAll(interactions);
			}
			myEntrySet.getEntries().add(myEntry);
			myEntrySets.add(myEntrySet);
		}

		return myEntrySets;

	}

	public void prepare(File input, String outputDir) {
		if (!outputDir.endsWith("/")) outputDir += "/";
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		entrySet = simplify(entrySet);
		List<EntrySet> ccs = getConnnectedComponents(entrySet);
		for (int i = 0; i < ccs.size(); i++) {
			File file = new File(outputDir + "cc_" + i + ".xml");
			NetworkUtils.writeNetwork(ccs.get(i), file);
			logger.info("Wrote connected component " + i + " to " + file.getPath());
		}
	}

	public EntrySet simplify(EntrySet entrySet) {

		// first, only copy the essential information (e.g. version number)
		EntrySet myEntrySet = new EntrySet();
		myEntrySet.setVersion(entrySet.getVersion());
		myEntrySet.setMinorVersion(entrySet.getMinorVersion());
		myEntrySet.setLevel(entrySet.getLevel());

		int numInteractionsInit = 0;
		int numInteractionsRemoved = 0;

		// now we want to add only the interactions and interactors from each entry
		for (Entry entry : entrySet.getEntries()) {

			numInteractionsInit += entry.getInteractions().size();

			Entry myEntry = new Entry();
			myEntry.setSource(entry.getSource());

			// only include interactions that have exactly 2 participants
			Collection<Interaction> myInteractions = myEntry.getInteractions();
			for (Interaction interaction : entry.getInteractions()) {
				Collection<Participant> participants = interaction.getParticipants();
				if (participants.size() == 2) {
					myInteractions.add(interaction);
				} else {
					numInteractionsRemoved++;
					logger.debug("Removing interaction " + interaction.getId() + " because it has " + participants.size() + " participants");
				}
			}

			myEntry.getInteractions().addAll(myInteractions);
			myEntry.getInteractors().addAll(entry.getInteractors());
			myEntrySet.getEntries().add(myEntry);
			
			logger.info("Removed " + numInteractionsRemoved + " interactions out of " + numInteractionsInit);

		}

		return myEntrySet;

	}

}
