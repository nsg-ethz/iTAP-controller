package ch.ethz.tik.sdnobfuscation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.internal.LinkDiscoveryManager;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.NodePortTuple;

public class ObfuscationLinkDiscoveryService extends LinkDiscoveryManager implements ILinkDiscoveryService {
	private Map<NodePortTuple,Link> linkForPort;
	
	public void updateLinkMapping() {
		
		if (linkForPort == null)
			linkForPort = new HashMap<NodePortTuple,Link>();
		
	    Iterator it = this.getLinks().entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        Link link = (Link) pair.getKey();
	        linkForPort.put(new NodePortTuple(link.getSrc(), link.getSrcPort()), link);
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	}
	
	public Link getLink(DatapathId sw, OFPort port) {
		if (linkForPort.containsKey(new NodePortTuple(sw,port)))
			return linkForPort.get(new NodePortTuple(sw,port));
		else
			return null;
	}
	
}
