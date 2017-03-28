package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.routing.Link;

public class ObfuscationLinkStateManager implements IFloodlightModule, 
		IObfuscationLinkStateManager {

	private Map<Link,ObfuscationLinkState> linkStates;
	private IObfuscationMaskManager oMaskManager;
	
	
	public void registerFlow(Link l, ObfuscatedFlow f) {
		if (!linkStates.containsKey(l))
			linkStates.put(l, new ObfuscationLinkState(l));
		
		linkStates.get(l).addFlow(f);
	}
	
	
	public int getNumberOfMaskUsages(Link l, long dst) {
		if (!linkStates.containsKey(l))
			return 0;
		else {
			return linkStates.get(l).getNumberOfMaskUsages(dst);
		}
	}
	
	
	public void resetNumberOfMaskUsages(long dst) {
		for (Link l : linkStates.keySet()) {
			linkStates.get(l).resetMaskIDstat(dst);
		}
	}
	
	
	
	
	public float getObservedEntropy(Link l, long dst) {
		if (!linkStates.containsKey(l))
			return ObfuscationPolicy.LEN_SRC_ID+ObfuscationPolicy.LEN_DST_ID;
		else {
			return linkStates.get(l).getObservedEntropy(dst);
		}
	}
	
	
	
	
	
	
	
	
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		
		linkStates = new HashMap<Link,ObfuscationLinkState>();
		oMaskManager = context.getServiceImpl(IObfuscationMaskManager.class);
		
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IObfuscationLinkStateManager.class);
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
		m.put(IObfuscationLinkStateManager.class, this);
		return m;
	}


	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IObfuscationMaskManager.class);
		return l;
	}


	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
	}


	public Map<Link, ObfuscationLinkState> getLinkStates() {
		return linkStates;
	} 

}
