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
 * @author dmyersturnbull
 */
package org.structnetalign;

import java.io.File;
import java.util.Collection;

import psidev.psi.mi.xml.PsimiXmlReader;
import psidev.psi.mi.xml.PsimiXmlReaderException;
import psidev.psi.mi.xml.PsimiXmlVersion;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Participant;

public class SimpleRun {

    private static final PsimiXmlVersion XML_VERSION = PsimiXmlVersion.VERSION_254;

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: SimpleRun input-file output-file");
			return;
		}
		File input = new File(args[0]);
		File output = new File(args[1]);
		run(input, output);
	}
	
	public static void run(File input, File output) {

        PsimiXmlReader reader = new PsimiXmlReader(XML_VERSION);
        EntrySet entrySet;
		try {
			entrySet = reader.read(input);
		} catch (PsimiXmlReaderException e) {
			throw new RuntimeException("Couldn't parse input file " + input, e);
		}

        for (Entry entry : entrySet.getEntries()) {
            for (Interaction interaction : entry.getInteractions()) {
            	Collection<Participant> participants = interaction.getParticipants();
            	for (Participant participant : participants) {
            		System.out.println(participant);
            	}
//               Collection<Confidence> confidences = interaction.getConfidences();
//               for (Confidence confidence : confidences) {
//            	   System.out.println(confidence.getValue());
//               }
            }
        }

	}

}
