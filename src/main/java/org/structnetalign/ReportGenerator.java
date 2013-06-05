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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.structnetalign.util.GraphImageWriter;

import edu.uci.ics.jung.graph.util.Pair;

public class ReportGenerator {

	public static class DegenerateSetEntry {
		public int v0;
		public String uniProtId0;
		public List<String> scopIds = new ArrayList<>();
		public List<Integer> ids = new ArrayList<>();
		public List<String> uniProtIds = new ArrayList<>();
		public List<String> pdbIds = new ArrayList<>();
		public List<Integer> interactionIds = new ArrayList<>();
		
		public List<Integer> getInteractionIds() {
			return interactionIds;
		}
		public int getV0() {
			return v0;
		}
		public String getUniProtId0() {
			return uniProtId0;
		}
		public List<String> getScopIds() {
			return scopIds;
		}
		public List<Integer> getIds() {
			return ids;
		}
		public List<String> getUniProtIds() {
			return uniProtIds;
		}
		public List<String> getPdbIds() {
			return pdbIds;
		}
	}
	
	public static class UpdateTableEntry {
		public int interaction;
		public Pair<Integer> ids;
		public Pair<String> uniProtIds;
		public Pair<String> pdbIds;
		public Pair<String> scopIds;
		public double before;
		public double after;
		public int getInteraction() {
			return interaction;
		}
		public Pair<Integer> getIds() {
			return ids;
		}
		public Pair<String> getUniProtIds() {
			return uniProtIds;
		}
		public double getBefore() {
			return before;
		}
		public double getAfter() {
			return after;
		}
		public Pair<String> getPdbIds() {
			return pdbIds;
		}
		public Pair<String> getScopIds() {
			return scopIds;
		}
	}
	
	private static class Properties {
		private String imageSource;
		private int nHomologies;
		private int nInteractions;
		private int nVertices;

		private boolean on = false;

		Map<String, Object> map = new HashMap<>();
		public void setImageSource(String imageSource) {
			this.imageSource = imageSource;
			on = true;
		}

		public void setNHomologies(int nHomologies) {
			this.nHomologies = nHomologies;
			on = true;
		}

		public void setNInteractions(int nInteractions) {
			this.nInteractions = nInteractions;
			on = true;
		}

		public void setNVertices(int nVertices) {
			this.nVertices = nVertices;
			on = true;
		}

		Map<String, Object> getMap() {
			if (!on) return null;
			map.put("img_src", imageSource);
			map.put("n_vertices", nVertices);
			map.put("n_homologies", nHomologies);
			map.put("n_interactions", nInteractions);
			return map;
		}
	}

	public void putInWeighted(String name, Object value) {
		value = format(value);
		weighted.map.put(name, value);
	}
	public void putInCrossed(String name, Object value) {
		value = format(value);
		crossed.map.put(name, value);
	}
	public void putInMerged(String name, Object value) {
		value = format(value);
		merged.map.put(name, value);
	}

	private static ReportGenerator instance;

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private Properties weighted = new Properties();
	private Properties crossed = new Properties();
	private Properties merged = new Properties();

	private final String DIR = "src/main/resources/web/";

	private String outputDir;
	private File outputFile;

	public static ReportGenerator getInstance() {
		return instance;
	}

	public static void setInstance(ReportGenerator instance) {
		ReportGenerator.instance = instance;
	}

	public ReportGenerator(File outputFile) {
		super();
		this.outputFile = outputFile;
		outputDir = outputFile.getParent();
		if (!outputDir.endsWith(File.separator)) outputDir += File.separator;
	}

	public void saveCrossed(CleverGraph graph) {
		File png = new File(outputDir + "crossed" + ".png");
		crossed.setImageSource(png.getName());
		crossed.setNVertices(graph.getVertexCount());
		crossed.setNHomologies(graph.getHomologyCount());
		crossed.setNInteractions(graph.getInteractionCount());
		GraphImageWriter writer = new GraphImageWriter();
		try {
			writer.writeGraph(graph, png);
		} catch (IOException e) {
			throw new RuntimeException("Could not save graph image file to " + png, e);
		}
	}

	public void saveMerged(CleverGraph graph) {
		File png = new File(outputDir + "merged" + ".png");
		merged.setImageSource(png.getName());
		merged.setNVertices(graph.getVertexCount());
		merged.setNHomologies(graph.getHomologyCount());
		merged.setNInteractions(graph.getInteractionCount());
		GraphImageWriter writer = new GraphImageWriter();
		try {
			writer.writeGraph(graph, png);
		} catch (IOException e) {
			throw new RuntimeException("Could not save graph image file to " + png, e);
		}
	}

	public void saveWeighted(CleverGraph graph) {
		File png = new File(outputDir + "weighted" + ".png");
		weighted.setImageSource(png.getName());
		weighted.setNVertices(graph.getVertexCount());
		weighted.setNHomologies(graph.getHomologyCount());
		weighted.setNInteractions(graph.getInteractionCount());
		GraphImageWriter writer = new GraphImageWriter();
		try {
			writer.writeGraph(graph, png);
		} catch (IOException e) {
			throw new RuntimeException("Could not save graph image file to " + png, e);
		}
	}

	private Map<String,Object> mainInfo = new TreeMap<String,Object>();

	/**
	 * Yes, I really am that OCD.
	 */
	private static Object format(Object value) {
		if (value == null) return null;
		if (value.getClass().isAssignableFrom(Double.class)) {
			double x = Math.abs((double) value);
			String minus = "";
			if (x < 0) minus = "−";
			if (Double.isInfinite(x)) {
				value = minus + "∞";
			} else {
				value = minus + x;
			}
		}
		return value;
	}

	public Object put(String key, Object value) {
		value = format(value);
		return mainInfo.put(key, value);
	}

	public void write() {

		logger.info("Saving final report to " + outputFile);

		VelocityEngine ve = new VelocityEngine();

		/*
		 * We need to do this because current Velocity doesn't work with Log4J version 2.
		 * So, we'll set up a logger ("chute") which just uses our log4J logger.
		 */
		LogChute chute = new LogChute() {
			@Override
			public void init(RuntimeServices arg0) throws Exception {

			}

			@Override
			public boolean isLevelEnabled(int level) {
				return true; // no way to tell
			}

			@Override
			public void log(int level, String message) {
				switch (level) {
				case LogChute.TRACE_ID:
					logger.trace(message);
					break;
				case LogChute.DEBUG_ID:
					logger.debug(message);
					break;
				case LogChute.INFO_ID:
					logger.info(message);
					break;
				case LogChute.WARN_ID:
					logger.warn(message);
					break;
				case LogChute.ERROR_ID:
					logger.error(message);
					break;
				default:
					logger.debug(message);
					break;
				}
			}

			@Override
			public void log(int level, String message, Throwable e) {
				switch (level) {
				case LogChute.TRACE_ID:
					logger.trace(message, e);
					break;
				case LogChute.DEBUG_ID:
					logger.debug(message, e);
					break;
				case LogChute.INFO_ID:
					logger.info(message, e);
					break;
				case LogChute.WARN_ID:
					logger.warn(message, e);
					break;
				case LogChute.ERROR_ID:
					logger.error(message, e);
					break;
				default:
					logger.debug(message, e);
					break;
				}
			}
		};
		ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, chute);
		Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, chute);
		Template template;
		try {
			ve.init();
			template = ve.getTemplate(DIR + "report.html.vm", "UTF-8");
		} catch (Exception e) {
			throw new RuntimeException("Couldn't initialize velocity engine for generating report", e);
		}

		VelocityContext context = new VelocityContext();

		// put any user-defined values
		for (Map.Entry<String,Object> entry : mainInfo.entrySet()) {
			context.put(entry.getKey(), entry.getValue());
		}

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

		// copy CSS
		try {
			FileUtils.copyFile(new File(DIR + "main.css"), new File(outputDir + "main.css"));
		} catch (IOException e) {
			logger.warn("Couldn't copy CSS file", e);
		}

		logger.info("Saved final report to " + outputFile);

	}

}
