package ch.ethz.tik.sdnobfuscation;

import net.floodlightcontroller.core.IOFSwitch;

import org.projectfloodlight.openflow.types.OFPort;

public class SwitchHostInfo {
	private IOFSwitch sw;
	private OFPort port;
	
	public SwitchHostInfo(IOFSwitch sw,OFPort port) {
		this.sw = sw;
		this.port = port;
	}
	
	public IOFSwitch getSwitch() {
		return this.sw;
	}
	
	public OFPort getPort() {
		return this.port;
	}
	
	public String toString() {
		return String.format("switch: %s, port: %s",sw.getId().toString(),port.toString());
	}
}
