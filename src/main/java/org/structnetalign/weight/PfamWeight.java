package org.structnetalign.weight;

public class PfamWeight implements RelationWeight {

	private String uniProtId1;
	private String uniProtId2;
	
	@Override
	public void setIds(String uniProtId1, String uniProtId2) throws WeightException {
		this.uniProtId1 = uniProtId1;
		this.uniProtId2 = uniProtId2;
	}

	@Override
	public double assignWeight(String uniProtId1, String uniProtId2) throws Exception {
		setIds(uniProtId1, uniProtId2);
		return call().getWeight();
	}

	@Override
	public WeightResult call() throws Exception {
		// in all honesty, I won't finish this
		// but we can put a TODO here anyway
		return new WeightResult(0, uniProtId1, uniProtId2);
	}

}
