package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;

public class ObfuscationMaskManager  implements IFloodlightModule, IObfuscationMaskManager{

	private Map<Long,ObfuscationMask> obfuscationMasks;
	private Map<BitSet,Long> obfuscationHeaders;
	private ILinkDiscoveryService linkDiscoveryService;
	private IObfuscationLinkStateManager oLinkStateManager;
	//private int currentMaskID;
	
	
	public ObfuscationMaskManager() {
		obfuscationMasks = new HashMap<Long,ObfuscationMask>();
	}
	
	
	public ObfuscationMask getObfuscationMask(long dst, IOFSwitch sw, Route route) {
		//System.out.println("get mask for " + dst);
		
		if (!checkMaskID(dst, route)) { // need new mask
			if (obfuscationMasks.containsKey(dst)) { // remove because mask must not be used anymore
				//System.out.println("*** remove mask !***");
				obfuscationMasks.remove(dst);
				oLinkStateManager.resetNumberOfMaskUsages(dst);
			}
		}
		
		if (!obfuscationMasks.containsKey(dst)) {
			System.out.println("*** create new mask ***");
			obfuscationMasks.put(dst, createNewObfuscationMask(dst));
		}
		return obfuscationMasks.get(dst);
	}
	
	private boolean checkMaskID(long dst, Route route) {
		return true;
		/*
		if (!obfuscationMasks.containsKey(dst))
			return false;
		else if (obfuscationMasks.get(dst).getAvailableSourceIDs().size() == 0)
			return false;
		else if (obfuscationMasks.get(dst).getAvailableDestinationIDs().size() == 0)
			return false;
		
		if ((route != null) && (route.getPath().size() >= 2)) {
			for (NodePortTuple l: route.getPath()) {
				Iterator<Link> it = linkDiscoveryService.getPortLinks().get(l).iterator();
				while (it.hasNext()) {
					Link link = it.next();
					if (oLinkStateManager.getNumberOfMaskUsages(link, dst) +1 > ObfuscationPolicy.UNICITY_DISTANCE)
						return false;
				}
			}
		}
		return true;*/
	}
	
	/**
	 * returns the minimum of the difference between the unicity distance and the number of mask usages for each link of the route using the current mask ID
	 * @param mask_id
	 * @param route
	 * @return
	 */
	public float getUnicityDiff(Ethernet packet, Route route) {
		IPv4 ip_pkt = (IPv4) packet.getPayload();
		
		int dst_port = 0;
		if (ip_pkt.getPayload() instanceof TCP) 
			dst_port = ((TCP)ip_pkt.getPayload()).getDestinationPort().getPort();
		if (ip_pkt.getPayload() instanceof UDP)
			dst_port = ((UDP)ip_pkt.getPayload()).getDestinationPort().getPort();
		
		long dst = ObfuscationPolicy.getEndpointIdentifier(false, packet.getDestinationMACAddress().getLong(), ip_pkt.getDestinationAddress().getInt(), dst_port);
		
		return getUnicityDiff(dst, route);
	}
	
	/**
	 * returns the minimum of the difference between the unicity distance and the number of mask usages for each link of the route
	 * @param mask_id
	 * @param route
	 * @return
	 */
	public float getUnicityDiff(long dst, Route route) {
		return 999;/*
		//System.out.println("getUnicityDiff for mask"+mask_id);
		float min = Float.MAX_VALUE;
		
		if ((route != null) && (route.getPath().size() >= 2)) {
			for (NodePortTuple l: route.getPath()) {
				Iterator<Link> it = linkDiscoveryService.getPortLinks().get(l).iterator();
				while (it.hasNext()) {
					Link link = it.next();
					//System.out.println("getObservedEntropy="+oLinkStateManager.getObservedEntropy(link, mask_id));
					//System.out.println("getNumberOfMaskUsages="+oLinkStateManager.getNumberOfMaskUsages(link, mask_id));
					min = Math.min(min, ObfuscationPolicy.UNICITY_DISTANCE-oLinkStateManager.getNumberOfMaskUsages(link, dst));
				}
			}
		}
		return min;*/
	}
	
	public ObfuscationMask getObfuscationMask(int id) {
		return obfuscationMasks.get(id);
	}
	
	public ObfuscationMask createNewObfuscationMask(long dst) {
		//System.out.println("create new mask for dst="+dst);
		ObfuscationPolicy oPolicy = new ObfuscationPolicy();
		
		ObfuscationMask oMask = new ObfuscationMask();
		oMask.setDst(dst);
		
		// choose which bits to use for src and dst ID
		ArrayList<Integer> obfuscationMaskBits = new ArrayList<Integer>();
		
		for (ObfuscationPolicy.Field field : oPolicy.getEncodeFields()) {
	        for (int i=field.getOffset(); i<(field.getOffset() + field.getLength()); i++) {
	        	obfuscationMaskBits.add(new Integer(i));
	        }
		}
		
/*
        for (int i=0; i<160; i++) {
        	obfuscationMaskBits.add(new Integer(i));
        }*/
		

        Collections.shuffle(obfuscationMaskBits);
        
        ArrayList<Integer> srcIdBits = new ArrayList<Integer>();
        for (int i=0; i<ObfuscationPolicy.LEN_SRC_ID; i++) {
            srcIdBits.add(new Integer(obfuscationMaskBits.get(i)));
        }
        oMask.setSrcIdMask(srcIdBits);
        
        ArrayList<Integer> dstIdBits = new ArrayList<Integer>();
        for (int i=ObfuscationPolicy.LEN_SRC_ID; i<ObfuscationPolicy.LEN_SRC_ID+ObfuscationPolicy.LEN_DST_ID; i++) {
            dstIdBits.add(new Integer(obfuscationMaskBits.get(i)));
        }
        oMask.setDstIdMask(dstIdBits);
		return oMask;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IObfuscationMaskManager.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService>
	getServiceImpls() {
		Map<Class<? extends IFloodlightService>,
		IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>,
		IFloodlightService>();
		m.put(IObfuscationMaskManager.class, this);
		return m;
	}


	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(ILinkDiscoveryService.class);
		l.add(IObfuscationLinkStateManager.class);
	    l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		oLinkStateManager = context.getServiceImpl(IObfuscationLinkStateManager.class);
		linkDiscoveryService = context.getServiceImpl(ILinkDiscoveryService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
	}

	public Map<Long, ObfuscationMask> getObfuscationMasks() {
		return obfuscationMasks;
	}

}
