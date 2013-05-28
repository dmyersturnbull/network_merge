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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.imageio.ImageIO;

import org.apache.commons.collections15.Transformer;
import org.structnetalign.CleverGraph;
import org.structnetalign.Edge;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class GraphImageWriter {

	private static NumberFormat nf = new DecimalFormat();
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}
	
	private double attraction = 0.7;

	private int fontSize = 20;

	private int height = 4000;

	private Color homologyColor = new Color(120, 0, 0);

	private float homologyDash[] = { 10.0f };

	private Color interactionColor = Color.BLACK;

	private float interactionDash[] = { 10000f };

	private int labelOffset = 25;

	private double repulsion = 0.9;

	private float thickness = 3f;

	private Color vertexColor = new Color(100, 200, 250);

	private int vertexSize = 60;

	private int width = 4000;

	private int xMargin = 60;

	private int yMargin = 60;

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err
					.println("Usage: GraphImageWriter input-interaction-graphml-file input-homology-graphml-file output-png-file");
			return;
		}
		writeImage(new File(args[0]), new File(args[1]), new File(args[2]));
	}

	public static void writeImage(File interaction, File homology, File output) {
		CleverGraph graph = GraphMLAdaptor.readGraph(interaction, homology);
		GraphImageWriter writer = new GraphImageWriter();
		try {
			writer.writeGraph(graph, output);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't write graph image to " + output, e);
		}
	}

	private static BufferedImage getImage(VisualizationImageServer<Integer, Edge> vv, Point2D center, Dimension d) {
		int width = vv.getWidth();
		int height = vv.getHeight();

		float scalex = (float) width / d.width;
		float scaley = (float) height / d.height;
		try {
			vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).scale(scalex, scaley, center);

			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = bi.createGraphics();
			graphics.setRenderingHints(vv.getRenderingHints());
			vv.paint(graphics);
			graphics.dispose();
			return bi;
		} finally {
			vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
		}
	}

	public GraphImageWriter() {
		this(4000, 4000);
	}

	public GraphImageWriter(int width, int height) {
		super();
		this.width = width;
		this.height = height;
	}

	public void setAttraction(double attraction) {
		this.attraction = attraction;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setHomologyColor(Color homologyColor) {
		this.homologyColor = homologyColor;
	}

	public void setHomologyDash(float[] homologyDash) {
		this.homologyDash = homologyDash;
	}

	public void setInteractionColor(Color interactionColor) {
		this.interactionColor = interactionColor;
	}

	public void setInteractionDash(float[] interactionDash) {
		this.interactionDash = interactionDash;
	}

	public void setLabelOffset(int labelOffset) {
		this.labelOffset = labelOffset;
	}

	public void setRepulsion(double repulsion) {
		this.repulsion = repulsion;
	}

	public void setThickness(float thickness) {
		this.thickness = thickness;
	}

	public void setVertexColor(Color vertexColor) {
		this.vertexColor = vertexColor;
	}

	public void setVertexSize(int vertexSize) {
		this.vertexSize = vertexSize;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setXMargin(int xMargin) {
		this.xMargin = xMargin;
	}

	public void setYMargin(int yMargin) {
		this.yMargin = yMargin;
	}

	public void writeGraph(CleverGraph graph, File file) throws IOException {
		UndirectedGraph<Integer, Edge> combined = graph.buildCombinedGraph();
		writeGraph(combined, file);
	}

	public void writeGraph(UndirectedGraph<Integer, Edge> graph, File file) throws IOException {

		Dimension dim = new Dimension(width, height);

		KKLayout<Integer, Edge> layout = new KKLayout<>(graph);
		// layout.setAttractionMultiplier(attraction);
		// layout.setRepulsionMultiplier(repulsion);
		// layout.setMaxIterations(1000);
		layout.setSize(new Dimension(width - xMargin, height - yMargin));

		VisualizationImageServer<Integer, Edge> vv = new VisualizationImageServer<>(layout, dim);

		vv.setPreferredSize(dim);

		Transformer<Integer, Paint> vertexPaint = new Transformer<Integer, Paint>() {
			@Override
			public Paint transform(Integer i) {
				return vertexColor;
			}
		};
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);

		final Stroke interactionStroke = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
				10.0f, interactionDash, 0.0f);
		final Stroke homologyStroke = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
				homologyDash, 0.0f);

		Transformer<Edge, Stroke> edgeStrokeTransformer = new Transformer<Edge, Stroke>() {
			@Override
			public Stroke transform(Edge edge) {
				if (edge instanceof HomologyEdge) {
					return homologyStroke;
				} else if (edge instanceof InteractionEdge) {
					return interactionStroke;
				}
				throw new IllegalArgumentException("Unknown edge type");
			}
		};
		vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);

		Transformer<Edge, String> edgeLabeler = new Transformer<Edge, String>() {
			@Override
			public String transform(Edge edge) {
				String color = "";
				if (edge instanceof InteractionEdge) {
					color = "rgb(" + interactionColor.getRed() + "," + interactionColor.getGreen() + ","
							+ interactionColor.getBlue() + ")";
				} else if (edge instanceof HomologyEdge) {
					color = "rgb(" + homologyColor.getRed() + "," + homologyColor.getGreen() + ","
							+ homologyColor.getBlue() + ")";
				} else {
					throw new IllegalArgumentException("Unknown edge type");
				}
				String prefix = "<html><font color=\"" + color + "\">";
				String suffix = "</font></html>";
				return prefix + nf.format(edge.getWeight()) + suffix;
			}
		};
		vv.getRenderContext().setEdgeLabelTransformer(edgeLabeler);

		Transformer<Integer, Font> vertexFont = new Transformer<Integer, Font>() {
			private final Font font = new Font("Serif", Font.BOLD, fontSize);

			@Override
			public Font transform(Integer edge) {
				return font;
			}
		};
		vv.getRenderContext().setVertexFontTransformer(vertexFont);

		Transformer<Edge, Font> edgeFont = new Transformer<Edge, Font>() {
			private final Font homologyFont = new Font("Serif", Font.BOLD, fontSize);
			private final Font interactionFont = new Font("Serif", Font.BOLD, fontSize);

			@Override
			public Font transform(Edge edge) {
				if (edge instanceof InteractionEdge) {
					return interactionFont;
				} else if (edge instanceof HomologyEdge) {
					return homologyFont;
				}
				throw new IllegalArgumentException("Unknown edge type");
			}
		};
		vv.getRenderContext().setEdgeFontTransformer(edgeFont);

		Transformer<Integer, Shape> vertexShape = new Transformer<Integer, Shape>() {
			private final Ellipse2D CIRCLE = new Ellipse2D.Double(-vertexSize / 2.0, -vertexSize / 2.0, vertexSize,
					vertexSize);

			@Override
			public Shape transform(Integer i) {
				return CIRCLE;
			}
		};
		vv.getRenderContext().setVertexShapeTransformer(vertexShape);

		Transformer<Edge, Paint> edgeDrawPaint = new Transformer<Edge, Paint>() {
			@Override
			public Paint transform(Edge edge) {
				if (edge instanceof InteractionEdge) {
					return interactionColor;
				} else if (edge instanceof HomologyEdge) {
					return homologyColor;
				}
				throw new IllegalArgumentException("Unknown edge type");
			}
		};
		vv.getRenderContext().setEdgeDrawPaintTransformer(edgeDrawPaint);

		vv.getRenderContext().setLabelOffset(labelOffset);
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Integer>());
		vv.getRenderContext().getEdgeLabelRenderer().setRotateEdgeLabels(false);
		vv.setOpaque(false);
		vv.setBackground(new Color(255, 255, 255, 0));

		writeImage(vv, file);

	}

	private void writeImage(VisualizationImageServer<Integer, Edge> vv, File file) throws IOException {
		Dimension dim = new Dimension(width, height);
		Point2D center = new Point2D.Double(width / 2.0 - xMargin / 2.0, height / 2.0 - yMargin / 2.0);
		BufferedImage image = getImage(vv, center, dim);
		ImageIO.write(image, "png", file);
	}
}
