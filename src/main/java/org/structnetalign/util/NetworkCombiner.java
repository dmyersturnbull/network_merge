package org.structnetalign.util;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import psidev.psi.mi.xml.PsimiXmlReader;
import psidev.psi.mi.xml.PsimiXmlReaderException;
import psidev.psi.mi.xml.PsimiXmlVersion;
import psidev.psi.mi.xml.PsimiXmlWriter;
import psidev.psi.mi.xml.PsimiXmlWriterException;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.Participant;

public class NetworkCombiner {

	private static final PsimiXmlVersion XML_VERSION = PsimiXmlVersion.VERSION_254;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: NetworkCombiner output-file probability input-files...");
			return;
		}
		final File output = new File(args[0]);
		final double probability = Double.parseDouble(args[1]);
		File[] inputs = new File[args.length-2];
		for (int i = 2; i < args.length; i++) {
			inputs[i-2] = new File(args[i]);
		}
		NetworkCombiner combiner = new NetworkCombiner();
		combiner.setProbability(probability);
		combiner.combine(output, inputs);
	}

	public void setPrepare(boolean prepare) {
		this.prepare = prepare;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	private static Random random = new Random();

	private boolean prepare = true;
	
	private double probability = 0.05;

	private Entry includeVertices(Entry entry, double probability) {

		Entry myEntry = new Entry();
		if (!prepare) {
			myEntry.setSource(entry.getSource());
			myEntry.getAttributes().addAll(entry.getAttributes());
			myEntry.getAvailabilities().addAll(entry.getAvailabilities());
			myEntry.getExperiments().addAll(entry.getExperiments());
		}

		Set<Integer> set = new HashSet<Integer>();
		Collection<Interactor> interactors = entry.getInteractors();
		for (Interactor interactor : interactors) {
			final double r = random.nextDouble();
			if (r >= probability) {
				set.add(interactor.getId());
				myEntry.getInteractors().add(interactor);
			}
		}

		// now add the edges
		interactions: for (Interaction interaction : entry.getInteractions()) {

			Collection<Participant> participants = interaction.getParticipants();
			if (prepare && participants.size() != 2) continue;

			for (Participant participant : participants) {
				final int id = participant.getInteractor().getId();
				if (!set.contains(id)) continue interactions;
			}
			
			myEntry.getInteractions().add(interaction);
			
		}
		
		return myEntry;

	}

	public void combine(File output, File... inputs) {

		EntrySet myEntrySet = new EntrySet();

		for (File input : inputs) {

			PsimiXmlReader reader = new PsimiXmlReader(XML_VERSION);
			EntrySet entrySet;
			try {
				entrySet = reader.read(input);
			} catch (PsimiXmlReaderException e) {
				throw new RuntimeException("Couldn't parse input file " + input, e);
			}

			for (Entry entry : entrySet.getEntries()) {
				Entry myEntry = includeVertices(entry, probability);
				myEntrySet.getEntries().add(myEntry);
			}

			reader = null; entrySet = null;
			System.gc(); // predictable GC times

		}

		PsimiXmlWriter psimiXmlWriter = new PsimiXmlWriter(XML_VERSION);
		try {
			psimiXmlWriter.write(myEntrySet, output);
		} catch (PsimiXmlWriterException e) {
			throw new RuntimeException("Couldn't write XML to " + output.getPath(), e);
		}
	}

}
