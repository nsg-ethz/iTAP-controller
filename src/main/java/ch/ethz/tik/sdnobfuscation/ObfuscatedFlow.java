package ch.ethz.tik.sdnobfuscation;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;

import ch.ethz.tik.sdnobfuscation.ObfuscationPolicy.Field;

public class ObfuscatedFlow {
	private ObfuscationMask obfuscationMask;
	private IPv4Address srcIpAddress;
	private IPv4Address dstIpAddress;
	private MacAddress srcMacAddress;
	private MacAddress dstMacAddress;
	private TransportPort srcPort;
	private TransportPort dstPort;
	private IpProtocol IpProtocol;
	
	private int sourceID;
	private int destinationID;
	
	private ObfuscationHeader oHeader;
	
	public ObfuscatedFlow() {
		this.srcPort = TransportPort.of(-1);
		this.dstPort = TransportPort.of(-1);
	}
	
	public void assignSrcDstID() {
		System.out.println(obfuscationMask);
		this.sourceID = obfuscationMask.querySourceId(srcMacAddress, this.srcIpAddress, this.IpProtocol, this.srcPort);
		this.destinationID = obfuscationMask.queryDestinationId(this.dstMacAddress, this.dstIpAddress, this.IpProtocol, this.dstPort);
	}
	
	
	public void buildObfuscationHeader() {
		oHeader = new ObfuscationHeader();
		oHeader.initRandomHeader();
		oHeader.encodeInHeader(obfuscationMask.getSrcIdMask(), sourceID);
		oHeader.encodeInHeader(obfuscationMask.getDstIdMask(), destinationID);
		
		ObfuscationPolicy oPolicy = new ObfuscationPolicy();
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_SRC))
			oHeader.encodeInHeader(ObfuscationPolicy.Field.MAC_SRC, this.srcMacAddress.getLong());
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_DST))
			oHeader.encodeInHeader(ObfuscationPolicy.Field.MAC_DST, this.dstMacAddress.getLong());
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.IP_SRC))
			oHeader.encodeInHeader(ObfuscationPolicy.Field.IP_SRC, this.srcIpAddress.getInt());
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.IP_DST))
			oHeader.encodeInHeader(ObfuscationPolicy.Field.IP_DST, this.dstIpAddress.getInt());
	}
	

	public MacAddress getSrcMacAddress() {
		return srcMacAddress;
	}

	public void setSrcMacAddress(MacAddress srcMacAddress) {
		this.srcMacAddress = MacAddress.of(srcMacAddress.getLong());
	}

	public MacAddress getDstMacAddress() {
		return dstMacAddress;
	}

	public void setDstMacAddress(MacAddress dstMacAddress) {
		this.dstMacAddress = MacAddress.of(dstMacAddress.getLong());
	}
	
	public ObfuscationMask getObfuscationMask() {
		return obfuscationMask;
	}

	public void setObfuscationMask(ObfuscationMask obfuscationMask) {
		this.obfuscationMask = obfuscationMask;
	}
	
	public IPv4Address getSrcIpAddress() {
		return srcIpAddress;
	}
	public void setSrcIpAddress(IPv4Address srcIpAddress) {
		this.srcIpAddress = IPv4Address.of(srcIpAddress.getInt());
	}
	public IPv4Address getDstIpAddress() {
		return dstIpAddress;
	}
	public void setDstIpAddress(IPv4Address dstIpAddress) {
		this.dstIpAddress = IPv4Address.of(dstIpAddress.getInt());
	}

	public TransportPort getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(TransportPort srcPort) {
		this.srcPort = TransportPort.of(srcPort.getPort());
	}

	public TransportPort getDstPort() {
		return dstPort;
	}

	public void setDstPort(TransportPort dstPort) {
		this.dstPort = TransportPort.of(dstPort.getPort());
	}


	public int getSourceID() {
		return sourceID;
	}

	public void setSourceID(int sourceID) {
		this.sourceID = sourceID;
	}

	public int getDestinationID() {
		return destinationID;
	}

	public void setDestinationID(int destinationID) {
		this.destinationID = destinationID;
	}

	@Override
	public String toString() {
		return "ObfuscatedFlow [obfuscationMask=" + obfuscationMask + ", srcIpAddress="
				+ srcIpAddress + ", dstIpAddress=" + dstIpAddress
				+ ", srcMacAddress=" + srcMacAddress + ", dstMacAddress="
				+ dstMacAddress + ", srcPort=" + srcPort + ", dstPort="
				+ dstPort + ", sourceID=" + sourceID + ", destinationID="
				+ destinationID + "]";
	}

	public ObfuscationHeader getObfuscationHeader() {
		return oHeader;
	}

	public IpProtocol getIpProtocol() {
		return IpProtocol;
	}

	public void setIpProtocol(IpProtocol ipProtocol) {
		IpProtocol = IpProtocol.of(ipProtocol.getIpProtocolNumber());
	}
}
