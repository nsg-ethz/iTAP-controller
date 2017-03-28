package ch.ethz.tik.sdnobfuscation;

import java.util.BitSet;
import java.util.Random;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;

public class ObfuscationHeader {
	
	/**
	 * concatenation of the following fields:
	 * field:	src MAC | dst MAC | src IP | dst IP
	 * bit:		0     47|48     95|96   127|128  159    
	 */
	protected BitSet obfuscatedHeaderValue;
	protected BitSet obfuscatedHeaderMask;

	protected TransportPort obfuscatedSrcPort;
	protected TransportPort obfuscatedDstPort;
	private Random rand;
	
	public ObfuscationHeader() {
		rand = new Random();
		
		obfuscatedHeaderValue = new BitSet(ObfuscationPolicy.LEN_HEADER);
		obfuscatedHeaderMask = new BitSet(ObfuscationPolicy.LEN_HEADER);
		obfuscatedSrcPort = TransportPort.of(0);
		obfuscatedDstPort = TransportPort.of(0);
	}
	
	public void setObfuscatedHeader(Match m) {
		try {
			encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.MAC_SRC.getOffset(), ObfuscationPolicy.Field.MAC_SRC.getLength(), m.get(MatchField.ETH_SRC).getLong());
			encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.MAC_DST.getOffset(), ObfuscationPolicy.Field.MAC_DST.getLength(), m.get(MatchField.ETH_DST).getLong());
			encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.IP_SRC.getOffset(), ObfuscationPolicy.Field.IP_SRC.getLength(), m.get(MatchField.IPV4_SRC).getInt());
			encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.IP_DST.getOffset(), ObfuscationPolicy.Field.IP_DST.getLength(), m.get(MatchField.IPV4_DST).getInt());
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println(m);
		}
	}
	
	public void setObfuscatedHeader(Ethernet eth) {
		try {
			//System.out.println(m.get(MatchField.ETH_SRC).getLong());
			//System.out.println(m.get(MatchField.ETH_SRC));
			encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.MAC_SRC.getOffset(), ObfuscationPolicy.Field.MAC_SRC.getLength(), eth.getSourceMACAddress().getLong());
			encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.MAC_DST.getOffset(), ObfuscationPolicy.Field.MAC_DST.getLength(), eth.getDestinationMACAddress().getLong());
			
			if (eth.getPayload() instanceof IPv4) {
				IPv4 ip_pkt = (IPv4) eth.getPayload();
				encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.IP_SRC.getOffset(), ObfuscationPolicy.Field.IP_SRC.getLength(), ip_pkt.getSourceAddress().getInt());
				encodeInBitSet(obfuscatedHeaderValue, ObfuscationPolicy.Field.IP_DST.getOffset(), ObfuscationPolicy.Field.IP_DST.getLength(), ip_pkt.getDestinationAddress().getInt());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println(eth);
		}
	}
	

	
	public void initRandomHeader() {
		for (int i=0; i<ObfuscationPolicy.LEN_HEADER; i++) {
			obfuscatedHeaderValue.set(i, rand.nextBoolean());
		}
		obfuscatedSrcPort = TransportPort.of(rand.nextInt(65535)+1);
		obfuscatedDstPort = TransportPort.of(rand.nextInt(65535)+1);
	}
	
	public void encodeInHeader(BitSet mask, long value) {
		encodeInBitSet(obfuscatedHeaderValue, mask, value);
	}
	
	public BitSet getMaskForField(ObfuscationPolicy.Field field) {
		int from, to;
		from = field.getOffset();
		to = from + field.getLength()-1;
		
		BitSet mask = new BitSet();
		mask.set(from, to, true);
		return mask;
	}
	
	public void encodeInHeader(ObfuscationPolicy.Field field, long value) {
		encodeInBitSet(obfuscatedHeaderValue, getMaskForField(field), value);
	}
	
	public void addMask(BitSet mask) {
		obfuscatedHeaderMask.or(mask);
	}
	
	public void clearMask() {
		obfuscatedHeaderMask.set(0, ObfuscationPolicy.LEN_HEADER-1, false);
	}
	
	public MacAddress getObfuscatedSrcMacValue() {		
		return MacAddress.of(extractFromBitSet(obfuscatedHeaderValue,ObfuscationPolicy.Field.MAC_SRC.getLength(),ObfuscationPolicy.Field.MAC_SRC.getOffset()));
	}
	
	public MacAddress getObfuscatedSrcMacMask() {		
		return MacAddress.of(extractFromBitSet(obfuscatedHeaderMask,ObfuscationPolicy.Field.MAC_SRC.getLength(),ObfuscationPolicy.Field.MAC_SRC.getOffset()));
	}
	
	public MacAddress getObfuscatedDstMacValue() {		
		return MacAddress.of(extractFromBitSet(obfuscatedHeaderValue,ObfuscationPolicy.Field.MAC_DST.getLength(),ObfuscationPolicy.Field.MAC_DST.getOffset()));
	}
	
	public MacAddress getObfuscatedDstMacMask() {		
		return MacAddress.of(extractFromBitSet(obfuscatedHeaderMask,ObfuscationPolicy.Field.MAC_DST.getLength(),ObfuscationPolicy.Field.MAC_DST.getOffset()));
	}
	
	public IPv4Address getObfuscatedSrcIpValue() {		
		return IPv4Address.of((int) extractFromBitSet(obfuscatedHeaderValue,ObfuscationPolicy.Field.IP_SRC.getLength(),ObfuscationPolicy.Field.IP_SRC.getOffset()));
	}
	
	public IPv4Address getObfuscatedSrcIpMask() {		
		return IPv4Address.of((int) extractFromBitSet(obfuscatedHeaderMask,ObfuscationPolicy.Field.IP_SRC.getLength(),ObfuscationPolicy.Field.IP_SRC.getOffset()));
	}
	
	public IPv4Address getObfuscatedDstIpValue() {		
		return IPv4Address.of((int) extractFromBitSet(obfuscatedHeaderValue,ObfuscationPolicy.Field.IP_DST.getLength(),ObfuscationPolicy.Field.IP_DST.getOffset()));
	}
	
	public IPv4Address getObfuscatedDstIpMask() {		
		return IPv4Address.of((int) extractFromBitSet(obfuscatedHeaderMask,ObfuscationPolicy.Field.IP_DST.getLength(),ObfuscationPolicy.Field.IP_DST.getOffset()));
	}
	
	
	private void encodeInBitSet(BitSet bitset, int offset, int length, long value) {
		BitSet mask = new BitSet(ObfuscationPolicy.LEN_HEADER);
		mask.set(offset, offset+length, true);
		encodeInBitSet(bitset, mask, value);
	}
	
	private void encodeInBitSet(BitSet bitset, BitSet mask, long value) {
		for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i+1)) {
			bitset.set(i, value % 2L == 1L);
			value = value >>> 1;
		 }
	}
	
	private long extractFromBitSet(BitSet bitset, int length, int offset) {
		long value = 0L;
	    for (int i = offset; i < offset+length; ++i) {
	      value += bitset.get(i) ? (1L << (i-offset)) : 0L;
	    }
	    return value;
	}
	
	public long getValueWithMask(BitSet mask) {
		return extractFromBitSet(obfuscatedHeaderValue, mask);
	}
	
	public long getValue(int length, int offset) {
		return extractFromBitSet(obfuscatedHeaderValue, length, offset);
	}
	
	private long extractFromBitSet(BitSet bitset, BitSet mask) {
		long value = 0L;
		long posValue = 1L;
		
		for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i+1)) {
			value += bitset.get(i) ? posValue : 0L;
			posValue = posValue << 1;
		 }
	    return value;
	    
	}

	@Override
	public String toString() {
		return "ObfuscationHeader [obfuscatedHeaderValue="
				+ obfuscatedHeaderValue + ", obfuscatedHeaderMask="
				+ obfuscatedHeaderMask + "]";
	}
	
	public TransportPort getObfuscatedSrcPort() {
		return obfuscatedSrcPort;
	}

	public TransportPort getObfuscatedDstPort() {
		return obfuscatedDstPort;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((obfuscatedHeaderValue == null) ? 0 : obfuscatedHeaderValue
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObfuscationHeader other = (ObfuscationHeader) obj;
		if (obfuscatedHeaderValue == null) {
			if (other.obfuscatedHeaderValue != null)
				return false;
		} else if (!obfuscatedHeaderValue.equals(other.obfuscatedHeaderValue))
			return false;
		return true;
	}
}
