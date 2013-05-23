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
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

public class ReportGenerator {

	private File outputFile;

	private final String DIR = "src/main/resources/web/";

	public ReportGenerator(File outputFile) {
		super();
		this.outputFile = outputFile;
	}

	public void write() {
		VelocityEngine ve = new VelocityEngine();
		Template template;
		try {
			ve.init();
			template = ve.getTemplate(DIR + "report.html.vm", "UTF-8");
		} catch (Exception e) {
			throw new RuntimeException("Couldn't initialize velocity engine for generating report", e);
		}
		VelocityContext context = new VelocityContext();
		Map<String, Object> weighted = new HashMap<>();
		context.put("weighted", weighted);

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
	}

}
