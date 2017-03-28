package ch.ethz.tik.sdnobfuscation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.packet.ARP;

import org.projectfloodlight.openflow.types.IPv4Address;

public class ArpRequestBuffer {
	private Map <IPv4Address , Set<ARP>> pendingRequests;
	
	public ArpRequestBuffer() {
		pendingRequests = new HashMap<IPv4Address , Set<ARP>>();
	}
	
	public void addRequest(ARP arpRequest) {
		if (!pendingRequests.containsKey(IPv4Address.of(arpRequest.getTargetProtocolAddress()))) {
			pendingRequests.put(IPv4Address.of(arpRequest.getTargetProtocolAddress()), new HashSet<ARP>());
		}
		pendingRequests.get(IPv4Address.of(arpRequest.getTargetProtocolAddress())).add(arpRequest);
	}
	
	public Set<ARP> getPendingRequests(IPv4Address ip) {
		if (pendingRequests.containsKey(ip))
			return pendingRequests.get(ip);
		else
			return new HashSet<ARP>();
	}
	
	public void removeRequest(ARP arpRequest) {
		if (pendingRequests.containsKey(IPv4Address.of(arpRequest.getTargetProtocolAddress()))) {
			pendingRequests.get(IPv4Address.of(arpRequest.getTargetProtocolAddress())).remove(arpRequest);
		}
	}

	@Override
	public String toString() {
		return "ArpRequestBuffer [pendingRequests=" + pendingRequests + "]";
	}
}
