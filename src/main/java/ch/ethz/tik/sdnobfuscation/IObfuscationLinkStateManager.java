package ch.ethz.tik.sdnobfuscation;

import java.util.Map;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.routing.Link;

public interface IObfuscationLinkStateManager extends IFloodlightService {
	public void registerFlow(Link l, ObfuscatedFlow f);
	public int getNumberOfMaskUsages(Link l, long dst);
	public float getObservedEntropy(Link l, long dst);
	public void resetNumberOfMaskUsages(long dst);
	public Map<Link, ObfuscationLinkState> getLinkStates();
}
