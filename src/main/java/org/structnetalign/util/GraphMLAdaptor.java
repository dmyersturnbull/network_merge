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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections15.Transformer;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.GraphMLMetadata;
import edu.uci.ics.jung.io.GraphMLReader;
import edu.uci.ics.jung.io.GraphMLWriter;

/**
 * An adaptor that converts between {@link UndirectedGraph graphs} for homology and interactions and their GraphML
 * representations.
 * 
 * @author dmyersturnbull
 * 
 */
public class GraphMLAdaptor {

	private static final String HOMOLOGY_WEIGHT_DESCRIPTION = "weight";
	private static final String HOMOLOGY_WEIGHT_LABEL = "w";
	private static final String INTERACTION_WEIGHT_DESCRIPTION = "weight";
	private static final String INTERACTION_WEIGHT_LABEL = "w";

	public static CleverGraph readGraph(File interactionFile, File homologyFile) {
		return readGraph(interactionFile.getPath(), homologyFile.getPath());
	}

	public static CleverGraph readGraph(String interactionFile, String homologyFile) {
		UndirectedGraph<Integer, InteractionEdge> interaction = readInteractionGraph(interactionFile);
		UndirectedGraph<Integer, HomologyEdge> homology = readHomologyGraph(interactionFile);
		return new CleverGraph(interaction, homology);
	}

	public static UndirectedGraph<Integer, HomologyEdge> readHomologyGraph(File file) {
		return readHomologyGraph(file.getPath());
	}

	public static UndirectedGraph<Integer, HomologyEdge> readHomologyGraph(String file) {
		GraphMLReader<UndirectedGraph<Integer, HomologyEdge>, Integer, HomologyEdge> reader;
		try {
			reader = new GraphMLReader<>();
		} catch (ParserConfigurationException | SAXException e) {
			throw new RuntimeException("Couldn't load GraphML file", e);
		}
		UndirectedGraph<Integer, HomologyEdge> graph = new UndirectedSparseGraph<>();
		try {
			reader.load(file, graph);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't load GraphML file", e);
		}
		GraphMLMetadata<HomologyEdge> weights = reader.getEdgeMetadata().get(HOMOLOGY_WEIGHT_LABEL);
		for (HomologyEdge edge : graph.getEdges()) {
			double score = Double.parseDouble(weights.transformer.transform(edge));
			edge.setScore(score);
		}
		return graph;
	}

	public static UndirectedGraph<Integer, InteractionEdge> readInteractionGraph(File file) {
		return readInteractionGraph(file.getPath());
	}

	public static UndirectedGraph<Integer, InteractionEdge> readInteractionGraph(String file) {
		GraphMLReader<UndirectedGraph<Integer, InteractionEdge>, Integer, InteractionEdge> reader;
		try {
			reader = new GraphMLReader<>();
		} catch (ParserConfigurationException | SAXException e) {
			throw new RuntimeException("Couldn't load GraphML file", e);
		}
		UndirectedGraph<Integer, InteractionEdge> graph = new UndirectedSparseGraph<>();
		try {
			reader.load(file, graph);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't load GraphML file", e);
		}
		GraphMLMetadata<InteractionEdge> weights = reader.getEdgeMetadata().get(INTERACTION_WEIGHT_LABEL);
		for (InteractionEdge edge : graph.getEdges()) {
			double score = Double.parseDouble(weights.transformer.transform(edge));
			edge.setProbability(score);
		}
		return graph;
	}

	public static void writeHomologyGraph(UndirectedGraph<Integer, HomologyEdge> graph, File file) {
		GraphMLWriter<Integer, HomologyEdge> writer = new GraphMLWriter<>();
		Transformer<HomologyEdge, String> edgeTransformer = new Transformer<HomologyEdge, String>() {
			@Override
			public String transform(HomologyEdge edge) {
				return String.valueOf(edge.getScore());
			}
		};
		writer.addEdgeData(HOMOLOGY_WEIGHT_LABEL, HOMOLOGY_WEIGHT_DESCRIPTION, null, edgeTransformer);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			writer.save(graph, bw);
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't save graph to " + file, e);
		}
	}

	public static void writeInteractionGraph(UndirectedGraph<Integer, InteractionEdge> graph, File file) {
		GraphMLWriter<Integer, InteractionEdge> writer = new GraphMLWriter<>();
		Transformer<InteractionEdge, String> edgeTransformer = new Transformer<InteractionEdge, String>() {
			@Override
			public String transform(InteractionEdge edge) {
				return String.valueOf(edge.getProbability());
			}
		};
		writer.addEdgeData(INTERACTION_WEIGHT_LABEL, INTERACTION_WEIGHT_DESCRIPTION, null, edgeTransformer);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			writer.save(graph, bw);
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't save graph to " + file, e);
		}
	}

}
