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

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PipelineProperties {

	private static final String INITIAL_CONF_LABEL = "struct-NA intial weighting";

	private static final String INITIAL_CONF_NAME = "struct-NA intial weighting";
	
	private static final String OUTPUT_CONF_NAME = "probability predicted by struct-NA";

	private static final String OUTPUT_CONF_LABEL = "struct-NA confidence";

	private static final String REMOVED_ATTRIBUTE_LABEL = "removed by Struct-NA";
	
	private static final String MAX_DISPLAY_DIGITS = "3";

	private static final String MAX_OUTPUT_DIGITS = "7";
	
	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private static PipelineProperties instance;
	
	private String initialConfLabel;
	
	private String initialConfName;
	
	private String outputConfLabel;
	
	private String outputConfName;

	private String removedAttributeLabel;
	

	static {
		Properties props = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream("databases.properties");
		try {
			props.load(stream);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open databases property file", e);
		}
		instance = new PipelineProperties();
		instance.initialConfLabel = props.getProperty("initial_conf_label", INITIAL_CONF_LABEL);
		instance.initialConfName = props.getProperty("initial_conf_name", INITIAL_CONF_NAME);
		instance.outputConfLabel = props.getProperty("output_conf_label", OUTPUT_CONF_LABEL);
		instance.outputConfName = props.getProperty("output_conf_name", OUTPUT_CONF_NAME);
		instance.removedAttributeLabel = props.getProperty("removed_attribute_label", REMOVED_ATTRIBUTE_LABEL);
		int maxOutputDigits = Integer.parseInt(props.getProperty("max_output_digits", MAX_OUTPUT_DIGITS));
		instance.outputFormatter = new DecimalFormat();
		instance.outputFormatter.setMaximumFractionDigits(maxOutputDigits);
		int maxDisplayDigits = Integer.parseInt(props.getProperty("max_display_digits", MAX_DISPLAY_DIGITS));
		instance.displayFormatter = new DecimalFormat();
		instance.displayFormatter.setMaximumFractionDigits(maxDisplayDigits);
	}

	private PipelineProperties() {
		
	}
	
	private NumberFormat displayFormatter;
	
	private NumberFormat outputFormatter;
	
	public String getInitialConfLabel() {
		return initialConfLabel;
	}

	public String getInitialConfName() {
		return initialConfName;
	}

	public String getOutputConfLabel() {
		return outputConfLabel;
	}

	public String getOutputConfName() {
		return outputConfName;
	}

	public NumberFormat getDisplayFormatter() {
		return displayFormatter;
	}

	public NumberFormat getOutputFormatter() {
		return outputFormatter;
	}

	public static PipelineProperties getInstance() {
		return instance;
	}

	public String getRemovedAttributeLabel() {
		return removedAttributeLabel;
	}

	@Override
	public String toString() {
		return "PipelineProperties [initialConfLabel=" + initialConfLabel + ", initialConfName=" + initialConfName
				+ ", outputConfLabel=" + outputConfLabel + ", outputConfName=" + outputConfName
				+ ", removedAttributeLabel=" + removedAttributeLabel + ", displayFormatter=" + displayFormatter
				+ ", outputFormatter=" + outputFormatter + "]";
	}

}
