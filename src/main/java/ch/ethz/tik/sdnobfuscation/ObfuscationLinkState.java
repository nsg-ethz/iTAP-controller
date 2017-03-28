package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.routing.Link;

@JsonSerialize(using=ObfuscationLinkStateJsonSerializer.class)
public class ObfuscationLinkState {
	private Link link;
	
	private Set<ObfuscatedFlow> installedFlows;

	private ConcurrentHashMap<Long, ConcurrentHashMap<Long , Integer>> observedDistribution; // <dst, <src ID|dst ID , weight>>
	private Map<Long, Integer> installedMasks; // <dst , cnt>
	
	public ObfuscationLinkState(Link l) {
		this.link = l;
		installedFlows = new HashSet<ObfuscatedFlow>();
		installedMasks = new HashMap<Long, Integer>();
		observedDistribution = new ConcurrentHashMap<Long, ConcurrentHashMap<Long , Integer>>();
	}
	
	public void addFlow(ObfuscatedFlow f) {
		installedFlows.add(f);
		incrementMaskIDstat(f.getObfuscationMask().getDst());
		addToObservedDistribution(f.getObfuscationMask().getDst(), f.getSourceID(), f.getDestinationID());
	}
	
	private void addToObservedDistribution(long dst, long src_id, long dst_id) {
		Long srcDstKey = (long) (src_id+(dst_id << ObfuscationPolicy.LEN_SRC_ID));
		
		if (!observedDistribution.containsKey(dst))
			observedDistribution.put(dst, new ConcurrentHashMap<Long , Integer>() );
		
		if (!observedDistribution.get(dst).containsKey(srcDstKey))
			observedDistribution.get(dst).put(srcDstKey, 1);
		else
			observedDistribution.get(dst).put(srcDstKey, observedDistribution.get(dst).get(srcDstKey)+1);

		//System.out.println(link+" -- d="+observedDistribution);
		//System.out.println("observed entropy: "+getObservedEntropy(mask_id));
	}
	
	
	public float getObservedEntropy(long dst) {
		if (!observedDistribution.containsKey(dst))
			return ObfuscationPolicy.LEN_SRC_ID+ObfuscationPolicy.LEN_DST_ID;
		
		ConcurrentHashMap<Long, Integer> dist = new ConcurrentHashMap<Long, Integer>(observedDistribution.get(dst));
		
		ArrayList<Integer> stats = new ArrayList<Integer>(dist.values());
		float p;
		float entropy = 0;
		
		float sum = 0;
		for(int x : stats)
		    sum += x;
		
		for (int x : stats) {
			p = (float) ((x + ObfuscationPolicy.INITIAL_WEIGHT_DISTRIBUTION) / (sum + Math.pow(2, ObfuscationPolicy.LEN_SRC_ID + ObfuscationPolicy.LEN_DST_ID) * ObfuscationPolicy.INITIAL_WEIGHT_DISTRIBUTION));
			
			if (p>0)
				entropy += p * Math.log(p)/Math.log(2);
		}
		
		// for unused values:
		p = (float) (( ObfuscationPolicy.INITIAL_WEIGHT_DISTRIBUTION) / (sum + Math.pow(2, ObfuscationPolicy.LEN_SRC_ID + ObfuscationPolicy.LEN_DST_ID) * ObfuscationPolicy.INITIAL_WEIGHT_DISTRIBUTION));
		
		if (p>0)
			entropy += (Math.pow(2, ObfuscationPolicy.LEN_SRC_ID + ObfuscationPolicy.LEN_DST_ID) - stats.size()) * p * Math.log(p)/Math.log(2);
		
		return entropy * (-1);
	}
	

	
	public float getUnicityDistance(long dst) {
		float obsEntropy = this.getObservedEntropy(dst);
		
		if ((ObfuscationPolicy.LEN_SRC_ID + ObfuscationPolicy.LEN_DST_ID - obsEntropy) <= 0)
			return Float.MAX_VALUE;
		else
			return (float) (ObfuscationPolicy.UNICITY_KEY_ENTROPY / (ObfuscationPolicy.LEN_SRC_ID + ObfuscationPolicy.LEN_DST_ID - obsEntropy));
	}
	
	
	private void incrementMaskIDstat(long dst) {
		if (!installedMasks.containsKey(dst))
			installedMasks.put(dst, 1);
		else
			installedMasks.put(dst, installedMasks.get(dst)+1);
	}
	
	public void resetMaskIDstat(long dst) {
		installedMasks.remove(dst);
	}
	
	
	public int getNumberOfMaskUsages(long dst) {
		if (!installedMasks.containsKey(dst))
			return 0;
		else
			return installedMasks.get(dst);
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public ConcurrentHashMap<Long, ConcurrentHashMap<Long, Integer>> getObservedDistribution() {
		return observedDistribution;
	}

	public Map<Long, Integer> getInstalledMasks() {
		return installedMasks;
	}
}
