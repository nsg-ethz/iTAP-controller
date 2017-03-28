package ch.ethz.tik.sdnobfuscation;

import java.util.Map;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Route;

public interface IObfuscationMaskManager extends IFloodlightService  {
	public ObfuscationMask getObfuscationMask(long dst, IOFSwitch sw, Route route);
	public ObfuscationMask getObfuscationMask(int id);
	//public int getCurrentMaskID();
	public Map<Long, ObfuscationMask> getObfuscationMasks();
	public float getUnicityDiff(long dst, Route route);
	public float getUnicityDiff(Ethernet packet, Route route);
	//public float getUnicityDiff(Route route);
}
