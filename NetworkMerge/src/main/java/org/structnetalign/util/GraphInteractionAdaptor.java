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
package org.structnetalign.util;

import java.io.File;

import org.structnetalign.CleverGraph;

import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.Interaction;
import edu.uci.ics.jung.graph.UndirectedGraph;

public class GraphInteractionAdaptor {

	public static UndirectedGraph<String,Interaction> toGraph(EntrySet entrySet) {
		return null;
	}
	
	public static void modifyProbabilites(EntrySet entrySet, CleverGraph graph) {
		
	}
	
	public static EntrySet readNetwork(File file) {
		return null;
	}
	
	public static void writeNetwork(EntrySet entrySet, File file) {
		
	}
	
}
