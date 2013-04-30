package org.structnetalign.util;

import java.io.File;
import java.util.Random;

import psidev.psi.mi.xml.PsimiXmlReader;
import psidev.psi.mi.xml.PsimiXmlReaderException;
import psidev.psi.mi.xml.PsimiXmlVersion;
import psidev.psi.mi.xml.PsimiXmlWriter;
import psidev.psi.mi.xml.PsimiXmlWriterException;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;

public class NetworkCombiner {

	private static final PsimiXmlVersion XML_VERSION = PsimiXmlVersion.VERSION_254;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: NetworkCombiner output-file probability input-files...");
			return;
		}
		final File output = new File(args[0]);
		final double prob = Double.parseDouble(args[1]);
		File[] inputs = new File[args.length-2];
		for (int i = 2; i < args.length; i++) {
			inputs[i-2] = new File(args[i]);
		}
		combine(output, prob, inputs);
	}

	public static void combine(File output, double probability, File... inputs) {

		EntrySet myEntrySet = new EntrySet();

		Random random = new Random();

		for (File input : inputs) {
			PsimiXmlReader reader = new PsimiXmlReader(XML_VERSION);
			EntrySet entrySet;
			try {
				entrySet = reader.read(input);
			} catch (PsimiXmlReaderException e) {
				throw new RuntimeException("Couldn't parse input file " + input, e);
			}
			for (Entry entry : entrySet.getEntries()) {
				final double r = random.nextDouble();
				if (r >= probability) {
					myEntrySet.getEntries().add(entry);
				}
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
