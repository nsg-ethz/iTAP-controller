package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using=ObfuscationMaskJsonSerializer.class)
public class ObfuscationMask {
	private long dst;
	private HashMap<String,HashMap<Integer,Integer>> sourceIDs;	//<key , <id,count>>
	private HashMap<String,HashMap<Integer,Integer>> destinationIDs;
	private List<Integer> availableSourceIDs;
	private List<Integer> availableDestinationIDs;
	private Random rand;
	private ObfuscationPolicy oPolicy;

	
	/**
	 *  masks the bits that are used to encode the source ID
	 */
	private BitSet srcIdMask = new BitSet(ObfuscationPolicy.LEN_HEADER);
	
	/**
	 *  masks the bits that are used to encode the destination ID
	 */
	private BitSet dstIdMask = new BitSet(ObfuscationPolicy.LEN_HEADER);

	
	public ObfuscationMask() {
		sourceIDs = new HashMap<String,HashMap<Integer,Integer>>();
		destinationIDs = new HashMap<String,HashMap<Integer,Integer>>();
		rand = new Random();
		oPolicy = new ObfuscationPolicy();
		
		availableSourceIDs = new ArrayList<Integer>();
		for (int i=0; i<Math.pow(2, ObfuscationPolicy.LEN_SRC_ID); i++)
			availableSourceIDs.add(i);
		Collections.shuffle(availableSourceIDs);
		
		availableDestinationIDs = new ArrayList<Integer>();
		for (int i=0; i<Math.pow(2, ObfuscationPolicy.LEN_DST_ID); i++)
			availableDestinationIDs.add(i);
		Collections.shuffle(availableDestinationIDs);
	}
	
	public BitSet getSrcIdMask() {
		return srcIdMask;
	}
	public void setSrcIdMask(BitSet srcIdMask) {
		this.srcIdMask = srcIdMask;
	}
	public void setSrcIdMask(ArrayList<Integer> usedBits) {
		for (int b : usedBits)
			this.srcIdMask.set(b,true);
	}
	public BitSet getDstIdMask() {
		return dstIdMask;
	}
	public void setDstIdMask(BitSet dstIdMask) {
		this.dstIdMask = dstIdMask;
	}
	public void setDstIdMask(ArrayList<Integer> usedBits) {
		for (int b : usedBits)
			this.dstIdMask.set(b,true);
	}
	

	/**
	 * returns the combination of all masks (mask id / src / dst) (=mask of all non-random bits)
	 * @return
	 */
	public BitSet getTotalMask() {
		BitSet totalMask = new BitSet(ObfuscationPolicy.LEN_HEADER);
		totalMask.or(srcIdMask);
		totalMask.or(dstIdMask);
		return totalMask;
	}
	

	public int querySourceId(MacAddress mac, IPv4Address ip, IpProtocol protocol, TransportPort port) {
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_SRC))
			mac = MacAddress.of(1);
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.IP_SRC))
			ip = IPv4Address.of(1);
		if (!(oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC) && oPolicy.doRewrite(port.getPort())))
			port = TransportPort.of(0);
		
		return queryHostId(0, mac, ip, protocol, port);
	}
	
	public int queryDestinationId(MacAddress mac, IPv4Address ip, IpProtocol protocol, TransportPort port) {
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_DST))
			mac = MacAddress.of(1);
		if (!oPolicy.doRewrite(ObfuscationPolicy.Field.IP_DST))
			ip = IPv4Address.of(1);
		if (!(oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST) && oPolicy.doRewrite(port.getPort())))
			port = TransportPort.of(0);
		
		return queryHostId(1, mac, ip, protocol, port);
	}
	
	public int getNextAvailableID(int type) {
		int id;
		if (type == 0) {
			id = availableSourceIDs.get(0);
			availableSourceIDs.remove(0);
		}
		else {
			id = availableDestinationIDs.get(0);
			availableDestinationIDs.remove(0);
		}
		return id;
	}
	
	/**
	 * 
	 * @param type = 0 for srcID, =1 for dstID
	 * @param mac
	 * @param ip
	 * @param port
	 * @return
	 */
	private int queryHostId(int type, MacAddress mac, IPv4Address ip, IpProtocol protocol, TransportPort port) {
		
		String key = mac.toString()+ip.toString()+protocol.toString()+port.toString();

		if ((type == 0) && sourceIDs.containsKey(key)) {
			if (rand.nextFloat()<oPolicy.getProbabilityNewHostID()) {
				int new_id = getNextAvailableID(type);
				registerNewID(type,key,new_id);
				System.out.println("*** new src ID ***");
			}
			
			List<Integer> keySet = new ArrayList<Integer>(sourceIDs.get(key).keySet());
			return keySet.get(rand.nextInt(keySet.size()));
		}
		else if ((type == 1) && destinationIDs.containsKey(key)) {
			if (rand.nextFloat()<oPolicy.getProbabilityNewHostID()) {
				int new_id = getNextAvailableID(type);
				registerNewID(type,key,new_id);
				System.out.println("*** new dst ID ***");
			}
			List<Integer> keySet = new ArrayList<Integer>(destinationIDs.get(key).keySet());
			return keySet.get(rand.nextInt(keySet.size()));
		}
		else if (type == 0) {
			int new_id = getNextAvailableID(type);
			registerNewID(type,key,new_id);
			return new_id;
		}
		else if (type == 1) {
			int new_id = getNextAvailableID(type);
			registerNewID(type,key,new_id);
			return new_id;
		}
		return 0;
	}
	
	private void registerNewID(int type, String key, int new_id) {
		if (type == 0) {
			if (!sourceIDs.containsKey(key))
				sourceIDs.put(key, new HashMap<Integer,Integer>());
			sourceIDs.get(key).put(new_id, 1);
		}
		else if (type == 1) {
			if (!destinationIDs.containsKey(key))
				destinationIDs.put(key, new HashMap<Integer,Integer>());
			destinationIDs.get(key).put(new_id, 1);
		}
	}
	

	/**
	 * returns the mask of all bits that are not used (and can therefore be random)
	 * @return
	 */
	public BitSet getRandomMask() {
		BitSet totalMask = this.getTotalMask();
		totalMask.flip(0, totalMask.length()-1);
		return totalMask;
	}
	@Override
	public String toString() {
		return "";
		/*return "ObfuscationMask [dst=" + dst + ", sourceIDs=" + sourceIDs
				+ ", destinationIDs=" + destinationIDs
				+ ", availableSourceIDs=" + availableSourceIDs
				+ ", availableDestinationIDs=" + availableDestinationIDs
				+ ", rand=" + rand + ", oPolicy=" + oPolicy + ", maskIdMask="
				+ maskIdMask + ", srcIdMask=" + srcIdMask + ", dstIdMask="
				+ dstIdMask + "]";*/
	}

	public HashMap<String, HashMap<Integer, Integer>> getSourceIDs() {
		return sourceIDs;
	}

	public int getSourceIDsSize() {
		int size = 0;
		
		Iterator<Entry<String, HashMap<Integer, Integer>>> it = sourceIDs.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<String, HashMap<Integer, Integer>> entry = it.next();
	        size += entry.getValue().size();
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
		return size;
	}

	public HashMap<String, HashMap<Integer, Integer>> getDestinationIDs() {
		return destinationIDs;
	}

	public int getDestinationIDsSize() {
		int size = 0;
		
		Iterator<Entry<String, HashMap<Integer, Integer>>> it = destinationIDs.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<String, HashMap<Integer, Integer>> entry = it.next();
	        size += entry.getValue().size();
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
		return size;
	}

	public List<Integer> getAvailableSourceIDs() {
		return availableSourceIDs;
	}

	public List<Integer> getAvailableDestinationIDs() {
		return availableDestinationIDs;
	}

	public long getDst() {
		return dst;
	}

	public void setDst(long dst) {
		this.dst = dst;
	}

}
