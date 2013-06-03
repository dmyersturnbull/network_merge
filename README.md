network_merge
=============

StructNA is a method to improve noisy protein–protein interaction (PPI) networks by identifying interactions that are conserved among homologs.
Homologs are identified in a species-independent way using structural rather than sequence information when available. Specifically, it uses from [SCOP](http://scop.berkeley.edu/) and structural alignment algorithms.

The goal is to determine the probability of interactions to make PPI networks more useful to researchers.
It is comparable to a network alignment algorithm but offers three advantages:
* It does not require that homology is one-to-one.
* It can identify homology relationships within the same species.
* It is based on structural information rather than sequence information.

StructNA is semi-stable but is nonetheless a work in progress currently not suitable for outside use.
It is distributed under the terms of the Apache License version 2.

### Build Status
[![Build Status](https://travis-ci.org/dmyersturnbull/network_merge.png)](https://travis-ci.org/dmyersturnbull/network_merge)
