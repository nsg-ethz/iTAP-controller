package ch.ethz.tik.sdnobfuscation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.topology.NodePortTuple;

public class ObfuscationSwitchState {
	private IOFSwitch sw;

	private Map<Long,List<NodePortTuple>> installedDestinationIDPaths; //<dst, path>
	private Set<ObfuscatedFlow> installedFlows;
	
	public ObfuscationSwitchState(IOFSwitch swi) {
		this.sw = swi;
		installedDestinationIDPaths = new HashMap<Long,List<NodePortTuple>>();
		installedFlows = new HashSet<ObfuscatedFlow>();
	}

	public void addFlow(ObfuscatedFlow f) {
		installedFlows.add(f);
	}
	
	public ObfuscatedFlow getFlowFromPacket(Ethernet eth) {
		System.out.println(eth.toString());
		
		ObfuscationHeader toCheck = new ObfuscationHeader();
		toCheck.setObfuscatedHeader(eth);
		System.out.println(toCheck.toString());
		
		for (ObfuscatedFlow f : installedFlows) {
			System.out.println(f.toString());
			if (f.getObfuscationHeader().equals(f))
				return f;
		}
		System.out.println("return null");
		return null;		
	}
	
	public boolean checkFlowFromPacket(Ethernet eth) {
		return (getFlowFromPacket(eth) != null);
	}
	
	public void addDestinationID(long dst, List<NodePortTuple> path) {
		installedDestinationIDPaths.put(dst, path);
	}
	
	public void removeDestinationID(long dst) {
		if (installedDestinationIDPaths.containsKey(dst))
			installedDestinationIDPaths.remove(dst);
	}
	
	public boolean checkDestinationID(long dst) {
		return (installedDestinationIDPaths.containsKey(dst));
	}
	
	public List<NodePortTuple> getPathForDestinationID(long dst) {
		if (checkDestinationID(dst)) 
			return installedDestinationIDPaths.get(dst);
		else
			return null;
	}

	public IOFSwitch getSwitch() {
		return sw;
	}

	public void setSwitch(IOFSwitch sw) {
		this.sw = sw;
	}

	public Set<ObfuscatedFlow> getInstalledFlows() {
		return installedFlows;
	}
}
