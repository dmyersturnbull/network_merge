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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.structure.align.ce.AbstractUserArgumentProcessor;
import org.structnetalign.weight.AtomCacheFactory;

/**
 * A command-line interface for Struct-NA's main component.
 * @author dmyersturnbull
 * @see PipelineManager
 */
public class CLI {

	public static final String PROGRAM_NAME = "Struct-NA";

	private static final Logger logger = LogManager.getLogger("org.structnetalign");

	private static final String NEWLINE = "\n";

	public static void main(String[] args) {

		Options options = getOptions();
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			printUsage(e.getMessage(), options);
			return;
		}
		
		runPipeline(cmd);
		
	}

	private static void runPipeline(CommandLine cmd) {
		String pdbDir = cmd.getOptionValue("pdb_dir");
		File input = new File(cmd.getOptionValue("input"));
		File output = new File(cmd.getOptionValue("output"));
		int nCores = cmd.hasOption("cores")? Integer.parseInt(cmd.getOptionValue("cores")) : PipelineManager.N_CORES;
		int xi = cmd.hasOption("xi")? Integer.parseInt(cmd.getOptionValue("xi")) : PipelineManager.XI;
		double tau = cmd.hasOption("tau")? Double.parseDouble(cmd.getOptionValue("tau")) : PipelineManager.TAU;
		double zeta = cmd.hasOption("zeta")? Double.parseDouble(cmd.getOptionValue("zeta")) : PipelineManager.ZETA;
		double beta = cmd.hasOption("beta")? Double.parseDouble(cmd.getOptionValue("beta")) : PipelineManager.BETA;
		boolean report = cmd.hasOption("report");
		boolean writeSteps = cmd.hasOption("write_steps");
		boolean noCross = cmd.hasOption("no_cross");
		boolean noMerge = cmd.hasOption("no_merge");
		runPipeline(pdbDir, nCores, input, output, tau, zeta, xi, beta, noCross, noMerge, writeSteps, report);
	}
	private static void runPipeline(String pdbDir, int nCores, File input, File output, double tau, double zeta, int xi, double beta, boolean noCross, boolean noMerge, boolean writeSteps, boolean report) {
		if (pdbDir != null) {
			System.setProperty(AbstractUserArgumentProcessor.PDB_DIR, pdbDir);
			AtomCacheFactory.setCache(pdbDir);
		}
		PipelineManager man = new PipelineManager();
		man.setXi(xi);
		man.setNCores(nCores);
		man.setTau(tau);
		man.setZeta(zeta);
		man.setBeta(beta);
		man.setReport(report);
		man.setWriteSteps(writeSteps);
		man.setNoCross(noCross);
		man.setNoMerge(noMerge);
		man.run(input, output);
	}

	/**
	 * Prints an error message for {@code e} that shows causes and suppressed messages recursively. Just a little more
	 * useful than {@code e.printStackTrace()}.
	 * 
	 * @param e
	 */
	public static void printError(Exception e) {
		System.err.println(printError(e, ""));
	}
		
	private static Options getOptions() {
		Options options = new Options();
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("The number of cores to use. Defaults to the number of available processors minus one.").isRequired(false)
				.create("cores"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("The directory containing cached PDB files. Defaults to the AtomCache default, which is probably in your system's temporary directory (e.g. /tmp). It is okay if this is an empty directory, but the directory must exist. You should set up a PDB cache path if you plan to run " + PROGRAM_NAME + " multiple times.").isRequired(false)
				.create("pdb_dir"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("The maximum search depth for traversal during crossing. If a vertex v shares an interaction edge x with vertex s, vertex v shares an interaction edge y with vertex t, and there is no path from u to t and no path from v to s, then x will be updated by y if and only if u and v are seperated by no more than xi homology edges (inclusive), AND s and t are seperated by no more than xi homology edges. Defaults to " + PipelineManager.XI + ". See paper for more details.").isRequired(false)
				.create("xi"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("A threshold probability prior for running the crossing process. Prior to crossing, any homology edge with probability less than tau will be removed. Defaults to " + PipelineManager.TAU + ".").isRequired(false)
				.create("tau"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("A threshold probability prior for running the merging process. Prior to merging, any homology edge with probability less than zeta will be removed. Note that this is performed after a similar process for tau, so zeta should be no greater than tau. Defaults to " + PipelineManager.ZETA + ".").isRequired(false)
				.create("zeta"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("The relative importance of databases over alignment. A homology edge will be assigned a weight of the sum of the alignment scores plus the beta times the sum of the database scores. Defaults to " + PipelineManager.BETA + ".").isRequired(false)
				.create("beta"));
		options.addOption(OptionBuilder.hasArg(false)
				.withDescription("If set, generates an HTML report page with accompanying graph visualizations in the specified directory.").isRequired(false)
				.create("report"));
		options.addOption(OptionBuilder.hasArg(false)
				.withDescription("Do not run the vertex degeneracy removal process.").isRequired(false)
				.create("no_merge"));
		options.addOption(OptionBuilder.hasArg(false)
				.withDescription("Do not run the probability update process.").isRequired(false)
				.create("no_cross"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("Required. The input PSI-MI25 XML file.").isRequired(true)
				.create("input"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("Required. The output PSI-MI25 XML file.").isRequired(true)
				.create("output"));
//		options.addOption(OptionBuilder.hasArg(true)
//				.withDescription("The name of the PSI-MI25 confidence short label to use to give the initial weighting (probability) of an interaction as input to " + CLI.PROGRAM_NAME + ". Defaults to " + GraphInteractionAdaptor.INITIAL_CONFIDENCE_LABEL).isRequired(false)
//				.create("input_conf"));
//		options.addOption(OptionBuilder.hasArg(true)
//				.withDescription("The default weighting (probability) of an interaction if the interaction does not have a weight described by input_conf. Defaults to " + GraphInteractionAdaptor.DEFAULT_PROBABILITY).isRequired(false)
//				.create("default_weight"));
//		options.addOption(OptionBuilder.hasArg(true)
//				.withDescription("The name of the PSI-MI25 confidence short label to use to describe the certainty of an interaction as determined by " + CLI.PROGRAM_NAME + ". Defaults to " + GraphInteractionAdaptor.CONFIDENCE_SHORT_LABEL).isRequired(false)
//				.create("output_conf_label"));
//		options.addOption(OptionBuilder.hasArg(true)
//				.withDescription("The name of the PSI-MI25 confidence full name to use to describe certainty of an interaction as determined by " + CLI.PROGRAM_NAME + ". Defaults to " + GraphInteractionAdaptor.CONFIDENCE_FULL_NAME).isRequired(false)
//				.create("output_conf_name"));
		options.addOption(OptionBuilder.hasArg(false)
				.withDescription("Output a GraphML file for each step.").isRequired(false)
				.create("write_steps"));
		options.addOption(OptionBuilder.hasArg(true)
				.withDescription("Skip the weighting process and use the specified GraphML file to indicate homology instead.").isRequired(false)
				.create("graphml_homology"));
		return options;
	}

	/**
	 * @see #printError(Exception)
	 */
	private static String printError(Exception e, String tabs) {
		StringBuilder sb = new StringBuilder();
		Throwable prime = e;
		while (prime != null) {
			if (tabs.length() > 0) sb.append(tabs + "Cause:" + NEWLINE);
			sb.append(tabs + prime.getClass().getSimpleName() + NEWLINE);
			if (prime.getMessage() != null) sb.append(tabs + prime.getMessage() + NEWLINE);
			if (prime instanceof Exception) {
				StackTraceElement[] trace = ((Exception) prime).getStackTrace();
				for (StackTraceElement element : trace) {
					sb.append(tabs + element.toString() + NEWLINE);
				}
			}
			prime = prime.getCause();
			tabs += "\t";
			sb.append(NEWLINE);
		}
		return sb.toString();
	}

	private static void printUsage(String note, Options options) {
		if (note != null) System.out.println(note);
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp("java -jar " + CLI.class.getSimpleName() + ".jar [options]", options);
	}

}
