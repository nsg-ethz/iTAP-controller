package ch.ethz.tik.sdnobfuscation;

import java.util.ArrayList;

import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;

public interface IObfuscationRoutingService extends IRoutingService {
	
	/**
	 * return all routes, if available 
	 * @param longSrcDpid
	 * @param longDstDpid
	 * @param tunnelEnabled
	 * @param maxLength: maximum length of route (depth of DFS search)
	 * @return
	 */
    public ArrayList<Route> getRoutes(DatapathId longSrcDpid, DatapathId longDstDpid, boolean tunnelEnabled, int maxLength);
}
