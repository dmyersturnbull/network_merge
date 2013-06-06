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
package org.structnetalign.weight;

import org.biojava.bio.structure.align.util.AtomCache;

/**
 * A factory that maintains an AtomCache for getting <a href="http://pdb.org">Protein Data Bank</a> structures.
 * @author dmyersturnbull
 *
 */
public class AtomCacheFactory {

	private static AtomCache cache;
	
	public static AtomCache getCache() {
		if (cache == null) setCache();
		return cache;
	}

	public static void setCache() {
		setCache(new AtomCache());
	}
	public static void setCache(String pdbDir) {
		setCache(new AtomCache(pdbDir, false));
	}
	
	public static void setCache(AtomCache cache) {
		AtomCacheFactory.cache = cache;
	}
	
}
