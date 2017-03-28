package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;


import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.topology.NodePortTuple;

public class ObfuscationSwitchStateManager  implements IFloodlightModule, IOFMessageListener, IObfuscationSwitchStateManager {

	private IFloodlightProviderService floodlightProvider;
	private Map<IOFSwitch,ObfuscationSwitchState> switchStates;
	private IObfuscationMaskManager oMaskManager;
	
	public void registerDestinationID(IOFSwitch sw, long dst, List<NodePortTuple> path) {
		if (!switchStates.containsKey(sw))
			switchStates.put(sw, new ObfuscationSwitchState(sw));
		switchStates.get(sw).addDestinationID(dst, path);
	}
	
	public List<NodePortTuple> getPathForDestinationID(IOFSwitch sw, long dst) {
		if (switchStates.containsKey(sw))
			return switchStates.get(sw).getPathForDestinationID(dst);
		else
			return null;
		
	}
	
	
	public void registerFlow(IOFSwitch sw, ObfuscatedFlow f) {
		if (!switchStates.containsKey(sw))
			switchStates.put(sw, new ObfuscationSwitchState(sw));
		
		switchStates.get(sw).addFlow(f);
		//System.out.println("+ state at sw "+sw.getId()+": "+switchStates.get(sw).getInstalledDestinationIDs());
	}
	
	public boolean checkDestinationID(IOFSwitch sw, long dst) {		
		return switchStates.containsKey(sw) && switchStates.get(sw).checkDestinationID(dst);
	}
	
	public ObfuscatedFlow getFlowFromPacket(IOFSwitch sw, Ethernet eth) {
		if (switchStates.containsKey(sw))
			return switchStates.get(sw).getFlowFromPacket(eth);
		else
			return null;
	}
	
	public boolean checkFlowFromPacket(IOFSwitch sw, Ethernet eth) {
		return switchStates.containsKey(sw) && switchStates.get(sw).checkFlowFromPacket(eth);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		System.out.println("flow expired: "+sw.toString() + msg.toString());
		
		//OFFlowRemoved flowRemoved = (OFFlowRemoved) msg;
		
		if (!switchStates.containsKey(sw))
			switchStates.put(sw, new ObfuscationSwitchState(sw));
		
		if (msg.getType() == OFType.FLOW_REMOVED) {
			OFFlowRemoved flowRemoved = (OFFlowRemoved) msg;
			System.out.println("flow expired: "+sw.toString() + "dst: " + flowRemoved.getCookie());
			long dst = flowRemoved.getCookie().getValue();
			ObfuscationHeader oHeader = new ObfuscationHeader();
			Match match = flowRemoved.getMatch();
			
			switchStates.get(sw).removeDestinationID(dst);
		
		}
		
		return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IObfuscationSwitchStateManager.class);
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
		m.put(IObfuscationSwitchStateManager.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IObfuscationMaskManager.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context .getServiceImpl(IFloodlightProviderService.class);
		switchStates = new HashMap<IOFSwitch,ObfuscationSwitchState>();
		oMaskManager = context.getServiceImpl(IObfuscationMaskManager.class);
		
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
		
	}
	
	
	@Override
	public String getName() {
		return ObfuscationSwitchStateManager.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

}
