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
package org.structnetalign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.structnetalign.util.GraphImageWriter;

public class ReportGenerator {

	private static class Properties {
		private String imageSource;
		private int nHomologies;
		private int nInteractions;
		private int nVertices;

		public void setImageSource(String imageSource) {
			this.imageSource = imageSource;
		}

		public void setNHomologies(int nHomologies) {
			this.nHomologies = nHomologies;
		}

		public void setNInteractions(int nInteractions) {
			this.nInteractions = nInteractions;
		}

		public void setNVertices(int nVertices) {
			this.nVertices = nVertices;
		}

		Map<String, Object> getMap() {
			Map<String, Object> map = new HashMap<>();
			map.put("img_src", imageSource);
			map.put("n_vertices", nVertices);
			map.put("n_homologies", nHomologies);
			map.put("n_interactions", nInteractions);
			return map;
		}
	}

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private final String DIR = "src/main/resources/web/";
	
	private Properties weighted = new Properties();
	private Properties crossed = new Properties();
	private Properties merged = new Properties();
	
	private String outputDir;
	private File outputFile;

	private int height = 1800;
	private int width = 1800;

	public ReportGenerator(File outputFile) {
		super();
		this.outputFile = outputFile;
		outputDir = outputFile.getParent();
		if (!outputDir.endsWith(File.separator)) outputDir += "/";
	}

	public void saveCrossed(CleverGraph graph) throws IOException {
		File png = new File(outputDir + "crossed" + ".png");
		crossed.setImageSource(png.getName());
		crossed.setNVertices(graph.getVertexCount());
		crossed.setNHomologies(graph.getHomologyCount());
		crossed.setNInteractions(graph.getInteractionCount());
		GraphImageWriter writer = new GraphImageWriter(width, height);
		writer.writeGraph(graph, png);
	}

	public void saveMerged(CleverGraph graph) throws IOException {
		File png = new File(outputDir + "merged" + ".png");
		merged.setImageSource(png.getName());
		merged.setNVertices(graph.getVertexCount());
		merged.setNHomologies(graph.getHomologyCount());
		merged.setNInteractions(graph.getInteractionCount());
		GraphImageWriter writer = new GraphImageWriter(width, height);
		writer.writeGraph(graph, png);
	}

	public void saveWeighted(CleverGraph graph) throws IOException {
		File png = new File(outputDir + "weighted" + ".png");
		weighted.setImageSource(png.getName());
		weighted.setNVertices(graph.getVertexCount());
		weighted.setNHomologies(graph.getHomologyCount());
		weighted.setNInteractions(graph.getInteractionCount());
		GraphImageWriter writer = new GraphImageWriter(width, height);
		writer.writeGraph(graph, png);
	}

	public void write() {
		
		logger.info("Saving final report to " + outputFile);
		
		VelocityEngine ve = new VelocityEngine();
		Template template;
		try {
			ve.init();
			template = ve.getTemplate(DIR + "report.html.vm", "UTF-8");
		} catch (Exception e) {
			throw new RuntimeException("Couldn't initialize velocity engine for generating report", e);
		}
		VelocityContext context = new VelocityContext();

		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		context.put("run_date", timestamp);

		Map<String, Object> weighted = this.weighted.getMap();
		context.put("weighted", weighted);

		Map<String, Object> crossed = this.crossed.getMap();
		context.put("crossed", crossed);

		Map<String, Object> merged = this.merged.getMap();
		context.put("merged", merged);

		StringWriter writer = new StringWriter();
		try {
			template.merge(context, writer);
		} catch (ResourceNotFoundException | ParseErrorException | MethodInvocationException | IOException e) {
			throw new RuntimeException("Couldn't merge velocity template", e);
		}

		try {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
				bw.write(writer.toString());
			}
		} catch (IOException e) {
			throw new RuntimeException("Couldn't write HTML to file " + outputFile.getPath(), e);
		}
		
		logger.info("Saved final report to " + outputFile);
		
	}

}
