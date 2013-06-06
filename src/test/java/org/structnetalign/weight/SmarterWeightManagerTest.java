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
package org.structnetalign.weight;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.structnetalign.HomologyEdge;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.util.Pair;



public class SmarterWeightManagerTest {

	private static final double PRECISION = 0.001;

	@Test
	public void testWithScop() {
		WeightCreator creator = new WeightCreator() {
			@Override
			public Weight nextWeight(int a, int b, String uniProtIdA, String uniProtIdB, int n, boolean isFail, Class<? extends Weight> failed) {
				if (n > 0) return null;
				Weight weight = new ScopRelationWeight();
				try {
					weight.setIds(a, b, uniProtIdA, uniProtIdB);
				} catch (WeightException e) {
					throw new RuntimeException(e);
				}
				return weight;
			}
			@Override
			public List<Weight> initialWeights(int a, int b, String uniProtIdA, String uniProtIdB) {
				List<Weight> weight = new ArrayList<Weight>(1);
				weight.add(nextWeight(a, b, uniProtIdA, uniProtIdB, 0, false, null));
				return weight;
			}
		};
		SmarterWeightManager manager = new SmarterWeightManager(creator, 2);
		UndirectedGraph<Integer,HomologyEdge> hom = WeightManagerTest.testSimple(manager);
		assertEquals("Wrong number of homology edges", 4, hom.getEdgeCount());

		assertEquals(0.4, hom.findEdge(4, 5).getWeight(), PRECISION);
		assertEquals(0.8, hom.findEdge(1, 2).getWeight(), PRECISION);
		assertEquals(0.1, hom.findEdge(1, 3).getWeight(), PRECISION);
	}

	@Test
	public void testMultiple() {
		WeightCreator creator = new WeightCreator() {
			@Override
			public Weight nextWeight(int a, int b, String uniProtIdA, String uniProtIdB, int n, boolean isFail, Class<? extends Weight> failed) {
				return null;
			}
			@Override
			public List<Weight> initialWeights(int a, int b, String uniProtIdA, String uniProtIdB) {
				List<Weight> weights = new ArrayList<Weight>(2);
				Weight scop = new ScopRelationWeight();
				try {
					scop.setIds(a, b, uniProtIdA, uniProtIdB);
				} catch (WeightException e) {
					throw new RuntimeException(e);
				}
				Weight ce = reallySimpleWeight(0.3, new HashSet<Pair<Integer>>());
				try {
					ce.setIds(a, b, uniProtIdA, uniProtIdB);
				} catch (WeightException e) {
					throw new RuntimeException(e);
				}
				weights.add(scop);
				weights.add(ce);
				return weights;
			}
		};
		SmarterWeightManager manager = new SmarterWeightManager(creator, 2);
		UndirectedGraph<Integer,HomologyEdge> hom = WeightManagerTest.testSimple(manager);
		assertEquals("Wrong number of homology edges", 15, hom.getEdgeCount());

		assertEquals(0.3+0.8-0.3*0.8, hom.findEdge(1, 2).getWeight(), PRECISION);
		assertEquals(0.3+0.1-0.1*0.3, hom.findEdge(1, 3).getWeight(), PRECISION);
		assertEquals(0.3, hom.findEdge(1, 4).getWeight(), PRECISION);
		assertEquals(0.3, hom.findEdge(1, 5).getWeight(), PRECISION);
		assertEquals(0.3+0.1-0.1*0.3, hom.findEdge(2, 3).getWeight(), PRECISION);
		assertEquals(0.3, hom.findEdge(2, 4).getWeight(), PRECISION);
		assertEquals(0.3, hom.findEdge(2, 5).getWeight(), PRECISION);
		assertEquals(0.3, hom.findEdge(3, 4).getWeight(), PRECISION);
		assertEquals(0.3, hom.findEdge(3, 5).getWeight(), PRECISION);
		assertEquals(0.3+0.4-0.3*0.4, hom.findEdge(4, 5).getWeight(), PRECISION);
	}

	@Test
	public void testWithFailure() {
		WeightCreator creator = new WeightCreator() {
			@Override
			public Weight nextWeight(int a, int b, String uniProtIdA, String uniProtIdB, int n, boolean isFail, Class<? extends Weight> failed) {
				if (n == 1) {
					Set<Pair<Integer>> failOn = new HashSet<>();
					// we won't get to add the 0.4
					failOn.add(new Pair<Integer>(4,5));
					Weight weight = reallySimpleWeight(0.4, failOn);
					try {
						weight.setIds(a, b, uniProtIdA, uniProtIdB);
					} catch (WeightException e) {
						throw new RuntimeException(e);
					}
					return weight;
				}
				return null;
			}
			@Override
			public List<Weight> initialWeights(int a, int b, String uniProtIdA, String uniProtIdB) {
				List<Weight> weights = new ArrayList<Weight>(1);
				Set<Pair<Integer>> failOn = new HashSet<>();
				failOn.add(new Pair<Integer>(2,3));
				failOn.add(new Pair<Integer>(4,5));
				Weight weight = reallySimpleWeight(0.25, failOn);
				try {
					weight.setIds(a, b, uniProtIdA, uniProtIdB);
				} catch (WeightException e) {
					throw new RuntimeException(e);
				}
				weights.add(weight);
				return weights;
			}
		};
		SmarterWeightManager manager = new SmarterWeightManager(creator, 2);
		UndirectedGraph<Integer,HomologyEdge> hom = WeightManagerTest.testSimple(manager);
		assertEquals("Wrong number of homology edges", 14, hom.getEdgeCount());

		assertEquals(0.55, hom.findEdge(1, 2).getWeight(), PRECISION);
		assertEquals(0.55, hom.findEdge(1, 3).getWeight(), PRECISION);
		assertEquals(0.55, hom.findEdge(1, 4).getWeight(), PRECISION);
		assertEquals(0.55, hom.findEdge(1, 5).getWeight(), PRECISION);
		assertEquals(0.55, hom.findEdge(2, 4).getWeight(), PRECISION);
		assertEquals(0.55, hom.findEdge(2, 5).getWeight(), PRECISION);
		assertEquals(0.55, hom.findEdge(3, 4).getWeight(), PRECISION);
		assertEquals(0.55, hom.findEdge(3, 5).getWeight(), PRECISION);
		assertEquals(0.4, hom.findEdge(2, 3).getWeight(), PRECISION);
		assertEquals(null, hom.findEdge(4, 5));
	}

	static Weight reallySimpleWeight(final double value, final Collection<Pair<Integer>> failOn) {
		return new Weight() {
			private int a;
			private int b;
			private String uniProtIdA;
			private String uniProtIdB;
			@Override
			public WeightResult call() throws Exception {
				if (failOn != null && failOn.contains(new Pair<Integer>(a,b))) throw new WeightException("", a, b, uniProtIdA, uniProtIdB, false, false);
				return new WeightResult(value, a, b, uniProtIdA, uniProtIdB, null);
			}
			@Override
			public double assignWeight(int v1, int v2, String uniProtId1, String uniProtId2) throws Exception {
				return value;
			}
			@Override
			public void setIds(int v1, int v2, String uniProtId1, String uniProtId2) throws WeightException {
				this.a = v1;
				this.b = v2;
				this.uniProtIdA = uniProtId1;
				this.uniProtIdB = uniProtId2;
			}
		};
	}
	
}
