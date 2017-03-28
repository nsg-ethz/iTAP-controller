package ch.ethz.tik.sdnobfuscation;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class ObfuscationDecoyController implements IFloodlightModule, IObfuscationDecoyController {


	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		

		IFloodlightProviderService floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		
		
		
		
		Thread thread = new Thread("initiate traffic") {
			  public void run(){
				  while(true) {
					    System.out.println("run by: " + getName());
					    
					    triggerTraffic(IPv4Address.of("212.243.100.177"), 1, 2);
			    
				        try {
							Thread.sleep(2000);
						} catch (InterruptedException e) { }
			    	  }
			      }
			   };
			
			thread.start();
			System.out.println(thread.getName());
		
	}
	
	private void triggerTraffic(IPv4Address host, int numPackets, int lenPackets) {
		String url = "http://"+host.toString()+"?packets="+numPackets+"&length="+lenPackets;
		
		URL obj = null;
		HttpURLConnection con = null;
		int responseCode = 0;
		
		try {
			obj = new URL(url);
		} catch (MalformedURLException e1) { e1.printStackTrace(); }
		
		try {
			con = (HttpURLConnection) obj.openConnection();
		} catch (IOException e1) { e1.printStackTrace(); }


		try {
			responseCode = con.getResponseCode();
		} catch (IOException e1) { e1.printStackTrace(); }
		
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
	}
	
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService>
	getServiceImpls() {
		Map<Class<? extends IFloodlightService>,
		IFloodlightService> m =
		new HashMap<Class<? extends IFloodlightService>,
		IFloodlightService>();
		m.put(IObfuscationDecoyController.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	    Collection<Class<? extends IFloodlightService>> l =
	        new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    return l;
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		
	}

}
