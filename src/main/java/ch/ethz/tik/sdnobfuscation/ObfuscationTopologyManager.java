package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyInstance;

import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;


public class ObfuscationTopologyManager extends net.floodlightcontroller.topology.TopologyManager implements IObfuscationRoutingService {
	protected Map<IPv4Address, SwitchHostInfo> IpToSwitch;
	protected Map<IPv4Address, MacAddress> IpToMac;
	
	public ObfuscationTopologyManager() {
		super();
		IpToSwitch = new ConcurrentHashMap<IPv4Address, SwitchHostInfo>();
		IpToMac = new ConcurrentHashMap<IPv4Address, MacAddress>();
	}
	
	public void updateTopologyMappings(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		if (eth.getPayload() instanceof IPv4) {
			IPv4 ip_pkt = (IPv4) eth.getPayload();
			
			if (ip_pkt.getSourceAddress().getInt() > 0) {
				IpToMac.put(ip_pkt.getSourceAddress(), eth.getSourceMACAddress());
				IpToSwitch.put(ip_pkt.getSourceAddress(),new SwitchHostInfo(sw,pi.getMatch().get(MatchField.IN_PORT)));
			}
		}
		else if (eth.getPayload() instanceof ARP) {
			ARP arp_pkt = (ARP) eth.getPayload();
			
			if (IPv4Address.of(arp_pkt.getSenderProtocolAddress()).getInt() > 0) {
			
				if (!IPv4Address.of(arp_pkt.getSenderProtocolAddress()).toString().contentEquals("10.0.0.111")) {// ignore crafted requests from switches 
					IpToMac.put(IPv4Address.of(arp_pkt.getSenderProtocolAddress()), eth.getSourceMACAddress());
					IpToSwitch.put(IPv4Address.of(arp_pkt.getSenderProtocolAddress()),new SwitchHostInfo(sw,pi.getMatch().get(MatchField.IN_PORT)));
				}
			}
		}
	}
	

	public boolean knowMacForIp(IPv4Address ipAddr) {
		return IpToMac.containsKey(ipAddr);
	}
	
	public boolean knowSwitchForIp(IPv4Address ipAddr) {
		return IpToSwitch.containsKey(ipAddr);
	}
	
	public MacAddress getMacForIp(IPv4Address ipAddr) {
		return IpToMac.get(ipAddr);
	}
	
	public SwitchHostInfo getSwitchForIp(IPv4Address ipAddr) {
		return IpToSwitch.get(ipAddr);
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(ITopologyService.class);
		l.add(IObfuscationRoutingService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService>
	getServiceImpls() {
		Map<Class<? extends IFloodlightService>,
		IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>,
		IFloodlightService>();
		// We are the class that implements the service
		m.put(ITopologyService.class, this);
		m.put(IObfuscationRoutingService.class, this);
		return m;
	}
	

	@Override
	public TopologyInstance getCurrentInstance(boolean tunnelEnabled) {
		if (tunnelEnabled)
			return currentInstance;
		else return this.currentInstanceWithoutTunnels;
	}
	
	@Override
	public TopologyInstance getCurrentInstance() {
		return this.getCurrentInstance(true);
	}
	
	
	 @Override
	    public ArrayList<Route> getRoutes(DatapathId srcDpid, DatapathId dstDpid, boolean tunnelEnabled) {
	        return getRoutes(srcDpid, dstDpid, tunnelEnabled, 99);
	    }
	 
	 public ArrayList<Route> getRoutes(DatapathId srcDpid, DatapathId dstDpid, boolean tunnelEnabled, int maxLength) {

	        ArrayList<DatapathId> v = new ArrayList<DatapathId>();
	        ArrayList<Route> routes = new ArrayList<Route>();

	        //routesList.clear();
	        ArrayList<Stack<NodePortTuple>> routesList = new ArrayList<Stack<NodePortTuple>>();

	        this.searchDfs(routesList,srcDpid,dstDpid,v,maxLength+1 /* +1 because #switches = #hops+1*/, new Stack<NodePortTuple>());
	        
	        
	        for(Stack<NodePortTuple> r:routesList){
	            ArrayList<NodePortTuple> ports = new ArrayList<NodePortTuple>();
	            for(NodePortTuple np:r){
	                ports.add(np);
	            }
	            Route path = new Route(srcDpid, dstDpid);
	            path.setPath(ports);
	            routes.add(path);
	        }
	      return routes;
	    }
	
	 // some parts from https://groups.google.com/a/openflowhub.org/forum/#!msg/floodlight-dev/XLodocgXYnE/lDtxLD2JxoUJ
	 private void searchDfs(ArrayList<Stack<NodePortTuple>> routesList, DatapathId current, DatapathId target, ArrayList<DatapathId> visited, int depth, Stack<NodePortTuple> route){
	        
	        //To avoid loops
		 if (visited.contains(current))
		 	return;
		 visited.add(current);
		 
		 if (current.equals(target)){
	            //Target has been reached -> add the route to the routes list
	            Stack<NodePortTuple> temp = new Stack<NodePortTuple>();
	            for (NodePortTuple np : route){
	                temp.add(np);
	            }
	            routesList.add(temp);
	            return;
	        }
		 
		 	if (depth == -1)
		 		return;
	                
	        Map<DatapathId, Set<Link>> allLinks = linkDiscoveryService.getSwitchLinks();
	        for(Link l:allLinks.get(current)){
	            //There are two links (bidirectional link)
	            if(l.getDst().equals(current))//we only need one of the two links to get to the next node
	                continue;
	            
	            if (visited.contains(l.getDst()))
	            	continue;
	            
	            //go to the next node into DFS subtree 
	            route.push(new NodePortTuple(l.getSrc(), l.getSrcPort()));
	            route.push(new NodePortTuple(l.getDst(), l.getDstPort()));
	            searchDfs(routesList, l.getDst(),target,(ArrayList<DatapathId>) visited.clone(),depth-1, route);
	            //search in subtree is completed -> return to upper level
	            route.pop();
	            route.pop();
	        }   
	    }
}
