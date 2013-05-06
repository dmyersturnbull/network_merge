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
import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;

import psidev.psi.mi.xml.PsimiXmlReader;
import psidev.psi.mi.xml.PsimiXmlReaderException;
import psidev.psi.mi.xml.PsimiXmlVersion;
import psidev.psi.mi.xml.PsimiXmlWriter;
import psidev.psi.mi.xml.PsimiXmlWriterException;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Participant;

public class NetworkUtils {

	private static final PsimiXmlVersion XML_VERSION = PsimiXmlVersion.VERSION_254;

	public static NavigableSet<Integer> getVertexIds(Interaction interaction) {
		Collection<Participant> participants = interaction.getParticipants();
		if (participants.size() != 2) throw new IllegalArgumentException(
				"Cannot handle interactions involving more than 2 participants");
		NavigableSet<Integer> set = new TreeSet<>();
		for (Participant participant : participants) {
			int id = participant.getInteractor().getId();
			set.add(id);
		}
		return set;
	}

	public static EntrySet readNetwork(File file) {
		PsimiXmlReader reader = new PsimiXmlReader(XML_VERSION);
		EntrySet entrySet;
		try {
			entrySet = reader.read(file);
		} catch (PsimiXmlReaderException e) {
			throw new RuntimeException("Couldn't parse input file " + file.getPath(), e);
		}
		return entrySet;
	}

	public static Entry skeletonClone(Entry entry) {
		Entry myEntry = new Entry();
		myEntry.setSource(entry.getSource());
		myEntry.getAttributes().addAll(entry.getAttributes());
		myEntry.getAvailabilities().addAll(entry.getAvailabilities());
		myEntry.getExperiments().addAll(entry.getExperiments());
		return myEntry;
	}

	public static EntrySet skeletonClone(EntrySet entrySet) {
		EntrySet myEntrySet = new EntrySet();
		myEntrySet.setVersion(entrySet.getVersion());
		myEntrySet.setMinorVersion(entrySet.getMinorVersion());
		myEntrySet.setLevel(entrySet.getLevel());
		return myEntrySet;
	}

	public static void writeNetwork(EntrySet entrySet, File file) {
		PsimiXmlWriter psimiXmlWriter = new PsimiXmlWriter(XML_VERSION);
		try {
			psimiXmlWriter.write(entrySet, file);
		} catch (PsimiXmlWriterException e) {
			throw new RuntimeException("Couldn't write XML to " + file.getPath(), e);
		}
	}

}
