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

import org.structnetalign.PipelineProperties;

import psidev.psi.mi.xml.model.Attribute;
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
	 * {@code threshold}.
	 * 
	 * @see {@link PipelineProperties#getOutputConfLabel()}
	 * @see {@link PipelineProperties#getOutputConfName()}
	 * @see {@link PipelineProperties#getRemovedAttributeLabel()}
	 */
	public static EntrySet trim(EntrySet entrySet, double threshold) {

		// we'll make a new EntrySet
		EntrySet myEntrySet = NetworkUtils.skeletonClone(entrySet);

		for (Entry entry : entrySet.getEntries()) {

			// make a new Entry and add it if it contains an interactor and/or an interaction
			Entry myEntry = NetworkUtils.skeletonClone(entry);
			boolean doAdd = false;

			// add interactors if they DON'T contain the "removed" annotation
			for (Interactor interactor : myEntry.getInteractors()) {
				Attribute attribute = NetworkUtils.getExistingAnnotation(interactor, PipelineProperties.getInstance()
						.getRemovedAttributeLabel());
				if (attribute == null) {
					myEntry.getInteractors().add(interactor);
					doAdd = true;
				}
			}

			// add interactions if they contain our confidence, and it is over the threshold
			for (Interaction interaction : myEntry.getInteractions()) {
				double value = NetworkUtils.getExistingConfidenceValue(interaction, PipelineProperties.getInstance()
						.getOutputConfLabel(), PipelineProperties.getInstance().getOutputConfName()); // let it throw a
																										// NFE
				if (value >= threshold) {
					myEntry.getInteractions().add(interaction);
					doAdd = true;
				}
			}

			// we add this way to avoid re-adding the entry (like could happen with a List
			if (doAdd) myEntrySet.getEntries().add(entry);

		}

		return myEntrySet;
	}

	/**
	 * Removes degenerate interactors and interactions, and every interaction whose Struct-NA confidence is below
	 * {@code threshold}. Writes the result to {@code output}.
	 * 
	 * @see {@link PipelineProperties#getOutputConfLabel()}
	 * @see {@link PipelineProperties#getOutputConfName()}
	 * @see {@link PipelineProperties#getRemovedAttributeLabel()}
	 */
	public static void trim(File input, File output, double threshold) {
		EntrySet entrySet = NetworkUtils.readNetwork(input);
		entrySet = trim(entrySet, threshold); // overwrite ref
		NetworkUtils.writeNetwork(entrySet, output);
	}

}
