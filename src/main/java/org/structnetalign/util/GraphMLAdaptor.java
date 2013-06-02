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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.structnetalign.CleverGraph;
import org.structnetalign.Edge;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.structnetalign.PipelineProperties;
import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphMLReader;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.io.graphml.EdgeMetadata;
import edu.uci.ics.jung.io.graphml.GraphMLReader2;
import edu.uci.ics.jung.io.graphml.GraphMetadata;
import edu.uci.ics.jung.io.graphml.GraphMetadata.EdgeDefault;
import edu.uci.ics.jung.io.graphml.HyperEdgeMetadata;
import edu.uci.ics.jung.io.graphml.NodeMetadata;

/**
 * An adaptor that converts between {@link UndirectedGraph graphs} for homology and interactions and their GraphML
 * representations.
 * 
 * @author dmyersturnbull
 * 
 */
public class GraphMLAdaptor {

	static final Logger logger = Logger.getLogger(NetworkCombiner.class.getName());

	private interface EdgeFactory<E extends Edge> {
		E createEmptyEdge();
	}

	private static final String WEIGHT_DESCRIPTION = "weight";
	private static final String WEIGHT_LABEL = "w";

	public static CleverGraph readGraph(File interactionFile, File homologyFile) {
		return readGraph(interactionFile.getPath(), homologyFile.getPath());
	}

	/**
	 * Just a simple graph reader. Does <em>not</em> read edge weights, etc.
	 * 
	 * @param file
	 * @return
	 */
	public static <V, E> UndirectedGraph<V, E> readGraph(String file) {
		GraphMLReader<UndirectedGraph<V, E>, V, E> reader;
		try {
			reader = new GraphMLReader<>();
		} catch (ParserConfigurationException | SAXException e) {
			throw new RuntimeException("Couldn't load GraphML file", e);
		}
		UndirectedGraph<V, E> graph = new UndirectedSparseGraph<>();
		try {
			reader.load(file, graph);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't load GraphML file", e);
		}
		return graph;
	}

	public static CleverGraph readGraph(String interactionFile, String homologyFile) {
		UndirectedGraph<Integer, InteractionEdge> interaction = readInteractionGraph(interactionFile);
		UndirectedGraph<Integer, HomologyEdge> homology = readHomologyGraph(homologyFile);
		return new CleverGraph(interaction, homology);
	}

	public static UndirectedGraph<Integer, HomologyEdge> readHomologyGraph(File file) {
		return readHomologyGraph(file.getPath());
	}

	public static UndirectedGraph<Integer, HomologyEdge> readHomologyGraph(final String file) {

		// get the transformers
		Transformer<GraphMetadata, Graph<Integer, HomologyEdge>> graphTransformer = getGraphTransformer();
		Transformer<NodeMetadata, Integer> vertexTransformer = getVertexTransformer();
		EdgeFactory<HomologyEdge> factory = new EdgeFactory<HomologyEdge>() {
			@Override
			public HomologyEdge createEmptyEdge() {
				return new HomologyEdge();
			}
		};
		Transformer<EdgeMetadata, HomologyEdge> edgeTransformer = getEdgeTransformer(factory);
		Transformer<HyperEdgeMetadata, HomologyEdge> hyperEdgeTransformer = getHyperEdgeTransformer();

		// now build the graph
		UndirectedGraph<Integer, HomologyEdge> graph;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			GraphMLReader2<Graph<Integer, HomologyEdge>, Integer, HomologyEdge> reader;
			reader = new GraphMLReader2<>(br, graphTransformer, vertexTransformer, edgeTransformer,
					hyperEdgeTransformer);
			try {
				graph = (UndirectedGraph<Integer, HomologyEdge>) reader.readGraph();
			} catch (GraphIOException e) {
				throw new RuntimeException("Couldn't load GraphML file", e);
			}
		} catch (IOException e1) {
			throw new RuntimeException("Couldn't load GraphML file", e1);
		}

		return graph;
	}

	public static UndirectedGraph<Integer, InteractionEdge> readInteractionGraph(File file) {
		return readInteractionGraph(file.getPath());
	}

	public static UndirectedGraph<Integer, InteractionEdge> readInteractionGraph(final String file) {

		// get the transformers
		Transformer<GraphMetadata, Graph<Integer, InteractionEdge>> graphTransformer = getGraphTransformer();
		Transformer<NodeMetadata, Integer> vertexTransformer = getVertexTransformer();
		EdgeFactory<InteractionEdge> factory = new EdgeFactory<InteractionEdge>() {
			@Override
			public InteractionEdge createEmptyEdge() {
				return new InteractionEdge();
			}
		};
		Transformer<EdgeMetadata, InteractionEdge> edgeTransformer = getEdgeTransformer(factory);
		Transformer<HyperEdgeMetadata, InteractionEdge> hyperEdgeTransformer = getHyperEdgeTransformer();

		// now build the graph
		UndirectedGraph<Integer, InteractionEdge> graph;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			GraphMLReader2<Graph<Integer, InteractionEdge>, Integer, InteractionEdge> reader;
			reader = new GraphMLReader2<>(br, graphTransformer, vertexTransformer, edgeTransformer,
					hyperEdgeTransformer);
			try {
				graph = (UndirectedGraph<Integer, InteractionEdge>) reader.readGraph();
			} catch (GraphIOException e) {
				throw new RuntimeException("Couldn't load GraphML file", e);
			}
		} catch (IOException e1) {
			throw new RuntimeException("Couldn't load GraphML file", e1);
		}

		return graph;
	}

	public static void writeHomologyGraph(UndirectedGraph<Integer, HomologyEdge> graph, File file) {

		GraphMLWriter<Integer, HomologyEdge> writer = new GraphMLWriter<>();

		// assign weights
		Transformer<HomologyEdge, String> edgeTransformer = getEdgeWeightTransformer();
		writer.addEdgeData(WEIGHT_LABEL, WEIGHT_DESCRIPTION, null, edgeTransformer);

		// assign edge Ids (needed by reader)
		Transformer<HomologyEdge, String> edgeIdTransformer = getEdgeIdTransformer();
		writer.setEdgeIDs(edgeIdTransformer);

		// write graph
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

		// assign weights
		Transformer<InteractionEdge, String> weightEdgeTransformer = getEdgeWeightTransformer();
		writer.addEdgeData(WEIGHT_LABEL, WEIGHT_DESCRIPTION, null, weightEdgeTransformer);

		// assign edge Ids (needed by reader)
		Transformer<InteractionEdge, String> edgeIdTransformer = getEdgeIdTransformer();
		writer.setEdgeIDs(edgeIdTransformer);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			writer.save(graph, bw);
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't save graph to " + file, e);
		}
	}

	private static <E extends Edge> Transformer<E, String> getEdgeWeightTransformer() {
		return new Transformer<E, String>() {
			@Override
			public String transform(E edge) {
				return PipelineProperties.getInstance().getDisplayFormatter().format(edge.getWeight());
			}
		};
	}

	private static <E extends Edge> Transformer<E, String> getEdgeIdTransformer() {
		return new Transformer<E, String>() {
			@Override
			public String transform(E edge) {
				return String.valueOf(edge.getId());
			}
		};
	}

	private static <E extends Edge> Transformer<EdgeMetadata, E> getEdgeTransformer(final EdgeFactory<E> factory) {
		return new Transformer<EdgeMetadata, E>() {
			private TreeSet<String> sourceDest = new TreeSet<String>();
			private boolean contains(String source, String target) {
				String hash = NetworkUtils.hash(source, target);
				boolean contains = sourceDest.contains(hash);
				sourceDest.add(hash);
				return contains;
			}
			@Override
			public E transform(EdgeMetadata metadata) {
				int id;
				try {
					id = Integer.parseInt(metadata.getId());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The graph has an edge whose Id is not a number");
				}
				double weight;
				try {
					weight = Double.parseDouble(metadata.getProperty(WEIGHT_LABEL));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The graph has an edge whose Id is not a number");
				}
				if (contains(metadata.getSource(), metadata.getTarget())) {
					throw new IllegalArgumentException("Graph already contains an edge (" + metadata.getSource() + ", " + metadata.getTarget());
				}
				E edge = factory.createEmptyEdge();
				edge.setId(id);
				edge.setWeight(weight);
				return edge;
			}
		};
	}

	private static <E extends Edge> Transformer<GraphMetadata, Graph<Integer, E>> getGraphTransformer() {
		return new Transformer<GraphMetadata, Graph<Integer, E>>() {
			@Override
			public Graph<Integer, E> transform(GraphMetadata metadata) {
				metadata.getEdgeDefault();
				if (metadata.getEdgeDefault().equals(EdgeDefault.DIRECTED)) {
					return new DirectedSparseGraph<Integer, E>();
				} else {
					return new UndirectedSparseGraph<Integer, E>();
				}
			}
		};
	}

	private static <E extends Edge> Transformer<HyperEdgeMetadata, E> getHyperEdgeTransformer() {
		return new Transformer<HyperEdgeMetadata, E>() {
			@Override
			public E transform(HyperEdgeMetadata metadata) {
				return null;
			}
		};
	}

	private static Transformer<NodeMetadata, Integer> getVertexTransformer() {
		return new Transformer<NodeMetadata, Integer>() {
			@Override
			public Integer transform(NodeMetadata metadata) {
				try {
					return Integer.parseInt(metadata.getId());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The graph has a vertex whose Id is not a number");
				}
			}
		};
	}

}
