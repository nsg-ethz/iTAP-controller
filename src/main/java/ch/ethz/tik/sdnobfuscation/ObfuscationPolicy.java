package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;
import java.util.List;

public class ObfuscationPolicy {

	private boolean doRandomRouting;
	private boolean chooseMinUnicityRoute;
	private int maxRoutingOverheadAbs;
	private float maxRoutingOverheadRel; // *100%
	private float probabilityNewHostID; // *100%
	
	private int flowIdleTimeout;
	private int flowHardTimeout;

	private List<ObfuscationPolicy.Field> rewriteFields; //fields that are rewritten
	private List<Integer> rewritePorts; //ports that are rewritten
	private List<ObfuscationPolicy.Field> encodeFields; //fields that are used to encode the src/dst ID
	
	public static final int LEN_HEADER = 160;
	
	public static final int LEN_SRC_ID = 20;
	public static final int LEN_DST_ID = 20;
	public static final int UNICITY_DISTANCE = 8;
	public static final float UNICITY_KEY_ENTROPY = (float) 99.08; // log2(150 choose 20)+log2(20 choose 10)
	
	public static final float INITIAL_WEIGHT_DISTRIBUTION = (float) 0.00050;	// average
	
	
	public ObfuscationPolicy() {
		rewriteFields = new ArrayList<ObfuscationPolicy.Field>();
		encodeFields = new ArrayList<ObfuscationPolicy.Field>();
		rewritePorts = new ArrayList<Integer>();
		
		rewriteFields.add(Field.MAC_SRC);
		rewriteFields.add(Field.MAC_DST);
		rewriteFields.add(Field.IP_SRC);
		rewriteFields.add(Field.IP_DST);
		//rewriteFields.add(Field.TP_SRC);
		//rewriteFields.add(Field.TP_DST);

		encodeFields.add(Field.MAC_SRC);
		encodeFields.add(Field.MAC_DST);
		encodeFields.add(Field.IP_SRC);
		encodeFields.add(Field.IP_DST);
		
		for (int i = 1; i <= 1024; i++)
			rewritePorts.add(i);
		
		
		doRandomRouting = false;
		maxRoutingOverheadAbs = 2;
		maxRoutingOverheadRel = 2; // *100%
		chooseMinUnicityRoute = false;
		
		probabilityNewHostID = (float) 0;

		//flowIdleTimeout = 60;
		//flowHardTimeout = 120;
		flowIdleTimeout = 999;
		flowHardTimeout = 0;
	}
	
	public boolean doRandomRouting() {
		return doRandomRouting;
	}

	public int getMaxRoutingOverheadAbs() {
		return (maxRoutingOverheadAbs>=0?maxRoutingOverheadAbs:Integer.MAX_VALUE);
	}

	public float getMaxRoutingOverheadRel() {
		return (maxRoutingOverheadRel>=0?maxRoutingOverheadRel:Float.MAX_VALUE);
	}
	
	public int getMaxRouteLength(int shortestPathLength) {
		return (int) Math.floor(Math.min(shortestPathLength+this.getMaxRoutingOverheadAbs(),shortestPathLength*(1+this.getMaxRoutingOverheadRel())));
	}

	public int getFlowIdleTimeout() {
		return flowIdleTimeout;
	}

	public int getFlowHardTimeout() {
		return flowHardTimeout;
	}

	public boolean chooseMinUnicityRoute() {
		return chooseMinUnicityRoute;
	}

	public float getProbabilityNewHostID() {
		return probabilityNewHostID;
	}
	
	
	public static long getEndpointIdentifier(boolean isSrc, long mac, int ip, int port) {
		int prime = 31;
		long result = 1;
		ObfuscationPolicy oPolicy = new ObfuscationPolicy();
		
		if ((isSrc && oPolicy.doRewrite(ObfuscationPolicy.Field.IP_SRC)) | (!isSrc && oPolicy.doRewrite(ObfuscationPolicy.Field.IP_DST)))
			result = prime * result + ip;
		if ((isSrc && oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_SRC)) | (!isSrc && oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_DST)))
			result = prime * result + mac;
		if ((isSrc && oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC)) | (!isSrc && oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST)))
			result = prime * result + port;
		return result;
	}
	
	public boolean doRewrite(Field field) {
		return rewriteFields.contains(field);
	}
	
	public boolean doRewrite(int port) {
		return rewritePorts.contains(port);
	}
	
	public boolean doEncode(Field field) {
		return encodeFields.contains(field);
	}
	
	public  List<ObfuscationPolicy.Field> getRewriteFields() {
		return this.rewriteFields;
	}
	
	public  List<ObfuscationPolicy.Field> getEncodeFields() {
		return this.encodeFields;
	}
	
	
	public enum Field {
		MAC_SRC	(0,48),
		MAC_DST	(48,48),
		IP_SRC	(96,32),
		IP_DST	(128,32),
		TP_SRC	(160,16),
		TP_DST	(176,16)
		;

		private final int length;
		private final int offset;
		
		Field(int offset, int length) {
			this.length = length;
			this.offset = offset;
		}
		
		public int getLength() {
			return this.length;
		}
		
		public int getOffset() {
			return this.offset;
		}
	}
}
















