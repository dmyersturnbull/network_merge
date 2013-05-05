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
package org.structnetalign.util;

import java.io.File;
import java.util.Collection;

import org.structnetalign.CleverGraph;
import org.structnetalign.InteractionEdge;

import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.Participant;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class GraphInteractionAdaptor {

	public static UndirectedGraph<Integer,InteractionEdge> toGraph(EntrySet entrySet, double defaultProbability) {
		
		UndirectedGraph<Integer,InteractionEdge> graph = new UndirectedSparseGraph<Integer,InteractionEdge>();
		
		for (Entry entry : entrySet.getEntries()) {
			
			// add the vertices
			Collection<Interactor> interactors = entry.getInteractors();
			for (Interactor interactor : interactors) {
				final int id = interactor.getId();
				graph.addVertex(id);
			}
			
			// now add the edges
			for (Interaction interaction : entry.getInteractions()) {
				
				Collection<Participant> participants = interaction.getParticipants();
				
				if (participants.size() != 2) throw new IllegalArgumentException("Cannot handle interactions involving more than 2 participants");

				// my God this is annoying
				int id1 = -1, id2 = -1;
				int i = 0;
				for (Participant participant : participants) {
					int id = participant.getInteractor().getId();
					switch(i) {
					case 0:
						id1 = id;
						break;
					case 1:
						id2 = id;
						break;
					}
					i++;
				}

				InteractionEdge edge = new InteractionEdge(interaction.getId(), defaultProbability);
				
				graph.addEdge(edge, id1, id2);
				
			}
			
		}
		
		return graph;
	}

	public static void modifyProbabilites(EntrySet entrySet, CleverGraph graph) {

	}

	public static EntrySet readNetwork(File file) {
		return null;
	}

	public static void writeNetwork(EntrySet entrySet, File file) {

	}

}
