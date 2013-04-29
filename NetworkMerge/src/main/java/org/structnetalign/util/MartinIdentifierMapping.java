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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biojava.bio.structure.scop.ScopDatabase;
import org.biojava.bio.structure.scop.ScopDomain;
import org.biojava.bio.structure.scop.ScopFactory;

/**
 * Perhaps the simplest mapping, but multi-chain domains are a problem.
 * @author dmyersturnbull
 */
public class MartinIdentifierMapping implements IdentifierMapping {

	private Map<String,String> uniProtToPdb;

	MartinIdentifierMapping() {
		try {
			uniProtToPdb = new HashMap<String,String>();
			File file = new File("src/main/resources/mappings/martin_pdb_uniprot_chain_map.lst");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\\w");
				uniProtToPdb.put(parts[2], parts[0] + "_" + parts[1]);
			}
			br.close();
		} catch (IOException e) {
			throw new IllegalStateException("Couldn't initialize " + MartinIdentifierMapping.class.getSimpleName()); // fatal
		}
	}

	@Override
	public String uniProtToPdb(String uniProtId) {
		return uniProtToPdb.get(uniProtId);
	}

	@Override
	public String uniProtToScop(String uniProtId) {
		final String pdb = uniProtToPdb.get(uniProtId);
		ScopDatabase scop = ScopFactory.getSCOP(ScopFactory.VERSION_1_75B);
		List<ScopDomain> domains = scop.getDomainsForPDB(pdb);
		if (domains.size() != 1) return null; // doesn't work for multi-chain or if there's no domain
		return domains.get(0).getScopId();
	}

}
