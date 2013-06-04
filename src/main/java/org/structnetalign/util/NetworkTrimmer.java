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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.structnetalign.PipelineProperties;

import psidev.psi.mi.xml.model.Attribute;
import psidev.psi.mi.xml.model.Confidence;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Interactor;

/**
 * A simple utility that removes interactions and interactors that Struct-NA marked as degenerate, and that removes
 * interactions whose Struct-NA confidence is below some threshold.
 * 
 * @author dmyersturnbull
 * @since 0.0.2
 */
public class NetworkTrimmer {

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.err.println("Usage: " + NetworkTrimmer.class.getSimpleName()
					+ " input-file output-file [threshold-confidence]");
			return;
		}
		double threshold = 0;
		if (args.length > 2) threshold = Double.parseDouble(args[2]);
		trim(new File(args[0]), new File(args[1]), threshold);
	}

	/**
	 * Removes degenerate interactors and interactions, and every interaction whose Struct-NA confidence is below
	 * {@code threshold}. Also removes the "initial weighting" confidence.
	 * 
	 * @see {@link PipelineProperties#getOutputConfLabel()}
	 * @see {@link PipelineProperties#getOutputConfName()}
	 * @see {@link PipelineProperties#getRemovedAttributeLabel()}
	 * @see {@link PipelineProperties#getInitialConfLabel()}
	 * @see {@link PipelineProperties#getInitialConfName()}
	 */
	public static EntrySet trim(EntrySet entrySet, double threshold) {

		logger.info("Trimming network with threshold " + threshold);

		// we'll make a new EntrySet
		EntrySet myEntrySet = NetworkUtils.skeletonClone(entrySet);

		int i = 0;
		for (Entry entry : entrySet.getEntries()) {

			// make a new Entry and add it if it contains an interactor and/or an interaction
			Entry myEntry = NetworkUtils.skeletonClone(entry);
			boolean doAdd = false;

			// add interactors if they DON'T contain the "removed" annotation
			for (Interactor interactor : entry.getInteractors()) {
				Attribute attribute = NetworkUtils.getExistingAnnotation(interactor, PipelineProperties.getInstance()
						.getRemovedAttributeLabel());
				if (attribute == null) {
					myEntry.getInteractors().add(interactor);
					doAdd = true;
					logger.debug("Kept interactor " + interactor.getId());
				} else {
					logger.debug("Discarded interactor " + interactor.getId() + " as non-representative degenerate");
				}
			}

			// add interactions if they contain our confidence, and it is over the threshold
			for (Interaction interaction : entry.getInteractions()) {

				Attribute attribute = NetworkUtils.getExistingAnnotation(interaction, PipelineProperties.getInstance()
						.getRemovedAttributeLabel());
				if (attribute != null) {
					logger.debug("Discarded interaction " + interaction.getId() + " as non-representative degenerate");
					continue;
				}

				// the confidence should exist
				Double value = NetworkUtils.getExistingConfidenceValue(interaction, PipelineProperties.getInstance()
						.getOutputConfLabel(), PipelineProperties.getInstance().getOutputConfName());
				if (value == null) {
					logger.warn("Struct-NA confidence for " + interaction.getId() + " does not exist, but it should");
					continue;
				}

				if (value >= threshold) {

					// remove the initial confidence
					Confidence init = NetworkUtils.getExistingConfidence(interaction, PipelineProperties.getInstance().getInitialConfLabel(), PipelineProperties.getInstance().getInitialConfName());
					if (init != null) interaction.getConfidences().remove(init);

					// add the interaction
					myEntry.getInteractions().add(interaction);
					doAdd = true;
					logger.debug("Kept interaction " + interaction.getId());

				} else {
					logger.debug("Discarded interaction " + interaction.getId() + " with value " + value + " < " + threshold);
				}

			}

			// we add this way to avoid re-adding the entry (like could happen with a List
			if (doAdd) {
				myEntrySet.getEntries().add(myEntry);
				logger.info("Added entry " + i);
			}
			i++;

		}

		return myEntrySet;
	}

	/**
	 * Removes degenerate interactors and interactions, and every interaction whose Struct-NA confidence is below
	 * {@code threshold}. Also removes the "initial weighting" confidence. Writes the result to {@code output}.
	 * 
	 * @see {@link PipelineProperties#getOutputConfLabel()}
	 * @see {@link PipelineProperties#getOutputConfName()}
	 * @see {@link PipelineProperties#getRemovedAttributeLabel()}
	 * @see {@link PipelineProperties#getInitialConfLabel()}
	 * @see {@link PipelineProperties#getInitialConfName()}
	 */
	public static void trim(File input, File output, double threshold) {
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		entrySet = trim(entrySet, threshold); // overwrite ref
		NetworkUtils.writeNetwork(entrySet, output);
	}

}
