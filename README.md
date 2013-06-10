Struct-NA
=========

Struct-NA is a method to improve noisy protein–protein interaction (PPI) networks by identifying interactions that are conserved among homologs.
The primary objective is to assign a confidence to each interaction based on its conservation among any homologs of its participants. The secondary objective is to merge analogous interactions to simplify the description of the network. The idea is that these networks will be more useful in research.
Homologs are identified in a species-independent way using structural rather than sequence information whenever it is available. Specifically, it uses the [Structural Classification of Proteins](http://scop.berkeley.edu/) and structural alignment algorithms.

The underlying method is comparable to a network alignment algorithm but offers three advantages:
* It does not require that homology is one-to-one.
* It can identify homology relationships within the same species.
* It is based on structural information rather than sequence information.

Struct-NA is semi-stable but is nonetheless a work in progress currently not suitable for outside use.
It is distributed under the terms of the Apache License version 2.

Who might use it?
-----------------------

There are two expected use cases:
* Distributors of high-throughput PPI networks who want a reliable computational method to estimate the confidence of each interaction.
* Researchers who are relying on PPI networks and who need estimates for interaction confidences. This includes researchers who have developed algorithms that accept PPIs as input (there are many).

How do I use it?
----------------------

Struct-NA reads a [Molecular Interaction XML Format](http://www.psidev.info/node/60) version 2.5 (MIF25) file (typically .mif25 or .mif) and outputs an output file in the same format.
This section assumes familiarity with this specification.

###Standard pipeline###
There are two critical components general users will need. Both are distributed as Java ARchive (JAR) files:

1. ```prepare.jar (NetworkPreparer.java)```, which is used to prepare a network for use by ```structna```. First, it removes unimolecular and multimolecular interactions, which Struct-NA doesn’t understand. It also assigns initial confidences to interactions. To use ```prepare```:
```java -jar prepare.jar input_network.mif25 prepared_network.mif25```
This will create a new file *prepared_network.mif25*.

2. ```structna.jar (CLI.java)```, which runs Struct-NA on a prepared MIF25 file. To use it:
```java -jar structna.jar -report -input prepared_network.mif25 -output ./output.mif25```
Interactions in the output MIF25 will be assigned a confidence, unless Struct-NA has marked them as degenerate; interactions and interactors are marked as degenerate with an annotation instead.
The *-report* switch is optional but recommended for new users. It creates a file at *(the location of output.mif25)/current-date-report/report.html* that details the results.
Run ```java -jar structna.jar --help``` to see the full options.

###Utilities##
Two useful but nonessential utilities are provided:

* ```combine.jar (NetworkCombiner.java)```, which can be used to mix and match networks, or to subsample a network or combination of networks. For example:
```java -jar combine.jar -output combined_network.mif25 -probability 1 -require_pdb -require_scop -require_fasta INPUT FILES```
The ```-probability``` parameter gives the probability that each interactor will be included. Interactions are then included only if all of their participants exist. The ```-require_pdb```, ```-require_scop```, and ```-require_fasta``` switches remove all interactors that do not have [Protein Data Bank](http://pdb.org/) structures, SCOP domains, and FASTA sequences at [NCBI](http://www.ncbi.nlm.nih.gov/), respectively. ```combine``` can be run before ```prepare```.
Try ```java -jar combine.jar --help``` to see the full options.

* ```trim.jar (NetworkTrimmer.java)```, which actually removes interactions and interactors marked with *removed by Struct-NA* from a network. Run:
```java -jar trim.jar result_network.mif25 trimmed_network.mif25```
This is useful if you are only interested in the most simplified form of a network for your research. It can also be useful for visualization.

###Using the results###
Running ```structna``` outputs a new MIF25 XML file that is identical to the first except for three new features:

1. A new “struct-NA confidence” [confidence](http://psidev.sourceforge.net/molecular_interactions//rel25/doc/#element_confidence_Link03B1CBD8) is included for most interactions. This is a floating-point number ranging from 0 to 1 that is Struct-NA’s estimate for the probability of that interaction.

2. A new annotation “removed by Struct-NA” has been added to some [interactions](http://psidev.sourceforge.net/molecular_interactions//rel25/doc/#element_interactionList_Link03B121D0) and [interactors](http://psidev.sourceforge.net/molecular_interactions//rel25/doc/#element_interactorList_Link03B11D10). It is present for every interactor belonging to a *degenerate set* that is not *representative*. For precise definitions of these terms, please refer to [the paper](https://github.com/dmyersturnbull/network_merge/blob/master/doc/description.pdf?raw=true). The value of this annotation is the representative interactor for that degenerate set. The annotation was also added to interactions for whom one or more participant is a non-representative member of degenerate set.

3. New interactions have been added to representative members of non-trivial degenerate sets. Specifically, one interaction was added for each interaction labeled for that degenerate set according to feature (2). These new interactions are those interactions that have been moved from non-representative interactors to representative interactors. 

###What general-use datasets can I run on?###
Here’s the current breakdown:

* [IntAct](http://www.ebi.ac.uk/intact/) seems to work very well! Please [report](https://github.com/dmyersturnbull/network_merge/issues/new) any issues.

* [DIP](http://dip.doe-mbi.ucla.edu/) does not correctly follow the MI25 specification, so parsing is problematic but may work. Please see [issue #7](https://github.com/dmyersturnbull/network_merge/issues/7).

* [BIOGRID](http://thebiogrid.org/) does not correctly follow the MI25 specification, and parsing doesn’t work at all. Please see [issue #8](https://github.com/dmyersturnbull/network_merge/issues/8).

More information will follow.

###How can I import the data into Cytoscape?###
[Cytoscape](http://www.cytoscape.org/) can import MIF25 files. In Cytoscape 3, just go to ```File → Import → Network → File```, and select the .mif25 file. You can use this to compare an input file with its StructNA‐generated output.
Note that Cytoscape won’t be able to read the GraphML files generated by the ```-write_steps``` option.

Understanding report files
------------------------------------
This section assumes the reader has read [the paper](https://github.com/dmyersturnbull/network_merge/blob/master/doc/description.pdf?raw=true).
The report.html file ```structna -report``` generates details the three steps major steps of the algorithm: “weighting”, “merging”, and “crossing”. Graphs are displayed for each. In these graphs, interactors are shown as vertices whose labels are the MIF25 interactor identifiers. Solid black lines denote interactions, and dashed red lines denote homology. The thickness of edges depends on their probabilities: the thicker an edge, the more likely it is.

###Weighted###
The first section simply shows the graph after homology edges have been added, with some basic information on the left. This is done after applying the threshold τ to the homology graph.

###Crossed###
The graph displayed is after probabilities of interactions have been updated. 
There are a couple confusing properties:
* *N edges updated*: the number of interactions whose probabilities were increased
* *N updates*: the number of interactions that were used to increase the probabilities of other interactions
The table that follows is a list of interactions that have been updated. Each row gives the MIF25 interaction Id that was updated, the initial probability as determined by ```prepare```, the probability that ```structna``` decided, and information about the interaction participants.

###Merged###
The graph displayed is after probabilities of interactions have been updated. This is done after applying the threshold ζ to the homology graph, which is why there are probably fewer dashed red edges in the graph..

To understand this section, you need a vague understanding of how this process works.
The term *degenerate set* is defined in the [paper](https://github.com/dmyersturnbull/network_merge/blob/master/doc/description.pdf). It is simply a set of vertices that form a clique and that all have precisely the same interactions. Non-trivial degeneracy (degeneracy involving more than one interaction) is the relation Struct-NA requires to merge multiple vertices into one. A single vertex is designated the *representative* of the degenerate set, and every other vertex is called *non-representative*. Each non-representative degenerate vertex is moved into its representative, and every interaction and homology edge incident to that vertex is likewise moved from to from the non-representative to the representative. (Strictly speaking, the original edges aren’t moved; they’re just copied. You can actually remove those edges using ```trim```.)

The table shows the exact degenerate sets and their representatives. Representatives are denoted “V0”.

Configuring
---------------------

###How do I change the initial scoring?###
Struct-NA uses a properties file at *src/main/resources/experiments.properties* that maps experiment names to initial scores (probabilities).
Simply modify this file and re-run *prepare*.

###How do I use a different database? ###
Database access information is stored in *src/main/resources/databases.properties*. You can modify this file to switch SCOP versions or use a different database URL.

###How do I change alignment scoring?###
This section concerns scoring of alignment methods and databases uses to identify homologous pairs.
Scoring parameters for individual databases and alignment methods are stored in properties files in *src/main/resources/weights/*. You can modify files here to make Needleman–Wunsch use a PAM matrix, SCOP superfamilies to be scored higher, or apply a harsh gap open penalty for Combinatorial Extension.

###How do I change the process used to identify homologs?###
This requires a source checkout.
The recommended way is to implement *WeightManager*, then:
```
PipelineManager manager = new PipelineManager();
manager.setWeightManager(new MyWeightManager()); // use your WeightManager here
manager.run(myInputFile, myOutputFile);
```
That should be it! The other major steps of Struct-NA, merging and crossing both are similarly easy to alter by implementing *MergeManager* and *CrossingManager*, respectively.

###It’s outputting too many log records!###
Struct-NA uses [Log4J](http://logging.apache.org/log4j/) version 2. Modify the file *src/main/resources/log4j2.xml* and change the attribute *level* from *trace* to *debug*, *info*, or *warn*. If you have a source checkout, you will want to modify both *src/main/resources/log4j2.xml* and *src/main/resources/log4j2-test.xml*.

The PSI-MI XML parser uses Log4J version 1 and is configured by default to show only warnings and errors. In general, these warnings should not be ignored. However, you can modify the file *src/main/resources/log4j.xml* to disable them.

###How do I obtain a checkout?###
The project is most easily built using [Maven](http://maven.apache.org/).
Here are three methods to obtain a checkout and build it with Maven:

First, you can try:
Run
```mvn scm:checkout -DconnectionUrl=https://github.com/dmyersturnbull/network_merge.git -DcheckoutDirectory=./StructNA```

Otherwise, you can use Git and then Maven:
```
git clone https://github.com/dmyersturnbull/network_merge.git
cd network_merge
mvn install
```

If you use [Eclipse](http://eclipse.org), you can install [m2eclipse](http://m2eclipse.codehaus.org/) with a Git m2eclipse discovery backend. Fro there, you can simply navigate to *create new → Checkout Maven projects from SCM* and input *https://github.com/dmyersturnbull/network_merge.git*.


Other information
---------------

###How does it work?###
There is [additional documentation](https://github.com/dmyersturnbull/network_merge/blob/master/doc/description.pdf) available. Like the code, this documentation is a work in progress. Unlike the code, it is not distributed under the Apache License (which is only applicable to software anyway).

###How did it get started?###
[Douglas Myers-Turnbull](https://github.com/dmyersturnbull) wrote it for a bioinformatics class at the [University of California, San Diego](http://ucsd.edu).

###How well is it working?###
[![Build Status](https://travis-ci.org/dmyersturnbull/network_merge.png)](https://travis-ci.org/dmyersturnbull/network_merge)

Please [report bugs](https://github.com/dmyersturnbull/network_merge/issues), because the developer will try to fix them.

Note that the software is probably not complete or stable enough for general use yet.

###How is it licensed?###
The software is distributed under the terms of the Apache License, version 2. The documentation, including this ReadMe and all files under the */doc* directory are provided only as a service to users, and may not be re-distributed modified or unmodified without express written permission from the author.
