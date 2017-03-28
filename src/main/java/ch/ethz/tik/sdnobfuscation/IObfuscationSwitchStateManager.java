package ch.ethz.tik.sdnobfuscation;

import java.util.List;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.topology.NodePortTuple;

public interface IObfuscationSwitchStateManager extends IFloodlightService  {
	public void registerDestinationID(IOFSwitch sw, long dst, List<NodePortTuple> path);
	public List<NodePortTuple> getPathForDestinationID(IOFSwitch sw, long dst);
	public void registerFlow(IOFSwitch sw, ObfuscatedFlow f);
	public boolean checkDestinationID(IOFSwitch sw, long dst);
	public ObfuscatedFlow getFlowFromPacket(IOFSwitch sw, Ethernet eth);
	public boolean checkFlowFromPacket(IOFSwitch sw, Ethernet eth);
}
