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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.structnetalign.CleverGraph;
import org.structnetalign.HomologyEdge;
import org.structnetalign.InteractionEdge;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import edu.uci.ics.jung.graph.UndirectedGraph;

public class TestUtils {

	static {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setTransformerFactory("org.apache.xalan.processor.TransformerFactoryImpl");
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreAttributeOrder(true);
	}

	public static class IgnoringDifferenceListener implements DifferenceListener {

		private List<Integer> ignoreValues;

		public IgnoringDifferenceListener() {
			this(Arrays.asList(DifferenceConstants.ATTR_VALUE.getId()));
		}

		public IgnoringDifferenceListener(List<Integer> ignoreValues) {
			this.ignoreValues = ignoreValues;
		}

		@Override
		public int differenceFound(Difference difference) {
			if (ignoreValues.contains(difference.getId())) {
				return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
			} else {
				return RETURN_ACCEPT_DIFFERENCE;
			}
		}

		@Override
		public void skippedComparison(Node control, Node test) {
		}

	}

	public static void printDetailedDiff(Diff diff, PrintStream ps) {
		DetailedDiff detDiff = new DetailedDiff(diff);
		for (Object object : detDiff.getAllDifferences()) {
			Difference difference = (Difference) object;
			ps.println(difference);
		}
	}

	public static boolean compareInteractionGraph(UndirectedGraph<Integer, InteractionEdge> graph, File expectedXmlFile) {
		File file = new File(RandomStringUtils.randomAlphanumeric(20) + ".xml.tmp");
		GraphMLAdaptor.writeInteractionGraph(graph, file);
		boolean ok = compareXml(expectedXmlFile, file);
		file.delete();
		return ok;
	}

	public static boolean compareHomologyGraph(UndirectedGraph<Integer, HomologyEdge> graph, File expectedXmlFile) {
		File file = new File(RandomStringUtils.randomAlphanumeric(20) + ".xml.tmp");
		GraphMLAdaptor.writeHomologyGraph(graph, file);
		boolean ok = compareXml(expectedXmlFile, file);
		file.delete();
		return ok;
	}
	
	public static boolean compareXml(File expectedFile, File actualFile) {
		try {
			FileReader expectedFr = new FileReader(expectedFile);
			FileReader actualFr = new FileReader(actualFile);
			Diff diff = new Diff(expectedFr, actualFr);
			// ignore order
			// look at element, id, and weight (weight is a nested element)
			diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
			final boolean isSimilar = diff.similar();
			if (!isSimilar) printDetailedDiff(diff, System.err);
			expectedFr.close();
			actualFr.close();
			return isSimilar;
		} catch (IOException | SAXException e) {
			throw new RuntimeException(e);
		}
	}

}
