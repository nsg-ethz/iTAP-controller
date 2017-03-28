package ch.ethz.tik.sdnobfuscation;

//mn --custom /home/floodlight/floodlight/mininet/WanTopo.py --topo wantopo --controller=remote,ip=127.0.0.1,port=6653 --switch ovsk,protocols=OpenFlow13
//mn --custom /home/floodlight/floodlight/mininet/XmlTopo.py --topo xmltopo --controller=remote,ip=127.0.0.1,port=6653 --switch ovsk,protocols=OpenFlow13
//mn --custom /home/floodlight/floodlight/mininet/LinearTopo.py --topo lineartopo --controller=remote,ip=127.0.0.1,port=6653 --switch ovsk,protocols=OpenFlow13


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.DHCP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.util.FlowModUtils;

public class ObfuscationController implements IFloodlightModule, IObfuscationController,
		IOFMessageListener {

	private IFloodlightProviderService floodlightProvider;
	private IOFSwitchService switchService;
	private static Logger log;
	
	private IObfuscationRoutingService routingService;
	private ILinkDiscoveryService linkDiscoveryService;
	
	private ObfuscationPolicy oPolicy;
	private ObfuscationTopologyManager oTopologyManager;
	private IObfuscationMaskManager oMaskManager;
	private IObfuscationSwitchStateManager oSwitchStateManager;
	private IObfuscationLinkStateManager oLinkStateManager;
	private ArpRequestBuffer arpRequestBuffer;
	private Random rand;


	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context .getServiceImpl(IFloodlightProviderService.class);
		log = LoggerFactory.getLogger(ObfuscationController.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		routingService = context.getServiceImpl(IObfuscationRoutingService.class);
		linkDiscoveryService = context.getServiceImpl(ILinkDiscoveryService.class);
		oSwitchStateManager = context.getServiceImpl(IObfuscationSwitchStateManager.class);
		oLinkStateManager = context.getServiceImpl(IObfuscationLinkStateManager.class);
		oTopologyManager = new ObfuscationTopologyManager();
		oMaskManager = context.getServiceImpl(IObfuscationMaskManager.class);
		oPolicy = new ObfuscationPolicy();
		arpRequestBuffer = new ArpRequestBuffer();
		rand = new Random();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		oTopologyManager.updateTopologyMappings(sw, (OFPacketIn) msg, cntx);
		
		//log.debug("receive {}",eth);
		
		if ((eth.getPayload() instanceof ARP)) {
			handleARP(sw, (OFPacketIn) msg, cntx);
		}
		else if (eth.getPayload() instanceof IPv4) {
			handleIP(sw, (OFPacketIn) msg, cntx);
		}
		else {
			//handleCbench(sw, (OFPacketIn) msg, cntx);
			//log.warn("could not handle packet {}",eth.toString());
		}
		return Command.CONTINUE;
	}
	
	private void handleIP(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		IPv4 ip_pkt = (IPv4) eth.getPayload();
		
		//log.debug("IP PacketIn {}->{}",new Object[] {ip_pkt.getSourceAddress().toString(), ip_pkt.getDestinationAddress().toString()});
		
		//TODO remove - ignore DHCP packets
		if (ip_pkt.getPayload().getPayload() instanceof DHCP)
			return;

		boolean isTCP= false;
		boolean isUDP= false;
		if (ip_pkt.getPayload() instanceof TCP)  {
			isTCP = true;
		}
		if (ip_pkt.getPayload() instanceof UDP) {
			isUDP = true;
		}
		
		log.debug("IP PacketIn {}->{}",new Object[] {ip_pkt.getSourceAddress().toString(), ip_pkt.getDestinationAddress().toString()});
		
		if (packetFromHost(sw, pi, cntx)) { // packet comes from a host -> obfuscate
			
			SwitchHostInfo srcSwitch = new SwitchHostInfo(sw,pi.getMatch().get(MatchField.IN_PORT));
			SwitchHostInfo dstSwitch;
			
			if (oTopologyManager.knowSwitchForIp(ip_pkt.getDestinationAddress())) {
				dstSwitch = oTopologyManager.getSwitchForIp(ip_pkt.getDestinationAddress());
				
				log.debug("search route {}->{}",new Object[] {srcSwitch.getSwitch().getId(), dstSwitch.getSwitch().getId()});
				
				Route route;
				if (srcSwitch.getSwitch().getId().equals(dstSwitch.getSwitch().getId())) {
					route = new Route(srcSwitch.getSwitch().getId(), dstSwitch.getSwitch().getId());
				}
				else {
					Route spRoute = routingService.getRoute(srcSwitch.getSwitch().getId(), dstSwitch.getSwitch().getId(),null);
					
					if ((spRoute != null) && (oPolicy.doRandomRouting())) {
						List<NodePortTuple> spRoutePath = spRoute.getPath();
						ArrayList<Route> allNonSpRoutes = routingService.getRoutes(srcSwitch.getSwitch().getId(), dstSwitch.getSwitch().getId(),false,oPolicy.getMaxRouteLength((int) Math.round(spRoutePath.size()/2)));
						
						int chooseRouteIdx = rand.nextInt(allNonSpRoutes.size());
						
						if (oPolicy.chooseMinUnicityRoute()) {
							float maxDiff = 0;
							float currDiff = 0;
							
							ArrayList<Integer> bestRoutes = new ArrayList<Integer>();
							
							for (int i = 0; i < allNonSpRoutes.size(); i++) {
					            currDiff = oMaskManager.getUnicityDiff(eth, allNonSpRoutes.get(i));
					            log.debug("currDiff "+currDiff+" at route "+allNonSpRoutes.get(i));
					            if (currDiff>maxDiff) {
					            	maxDiff = currDiff;
					            	bestRoutes.clear();
					            	bestRoutes.add(i);
					            }
					            else if ((currDiff == maxDiff) && (currDiff>0)) {
					            	bestRoutes.add(i);
					            }
					        }
							log.debug("maxDiff {}",maxDiff);
							log.debug("bestRoutes {}",bestRoutes);
							
							if (bestRoutes.size() > 0)
								chooseRouteIdx = bestRoutes.get(rand.nextInt(bestRoutes.size())); // choose one of the best at random								
						}
						
						route = allNonSpRoutes.get(chooseRouteIdx);
					}
					else
						route = spRoute;
				}
				
				if ((route == null) && (!srcSwitch.getSwitch().getId().equals(dstSwitch.getSwitch().getId()))) {
					log.warn("no route found {}->{}",new Object[] {srcSwitch.getSwitch().getId(), dstSwitch.getSwitch().getId()});
				}
				else {
					log.debug("route computed: {}",route.getPath().toString());
					
					
					OFFactory factory = switchService.getSwitch(sw.getId()).getOFFactory();
					
					if (srcSwitch.getSwitch().getId().equals(dstSwitch.getSwitch().getId())) { // src and dst host are connected to the same switch -> no obfuscation

						ArrayList<OFAction> actions = new ArrayList<OFAction>();
						OFFlowAdd.Builder fab = factory.buildFlowAdd();
						fab.setBufferId(OFBufferId.NO_BUFFER);
						fab.setPriority(FlowModUtils.PRIORITY_MAX);
						
						fab.setMatch(factory
								.buildMatch()
								.setExact(MatchField.ETH_DST, eth.getDestinationMACAddress())
								.setExact(MatchField.ETH_TYPE, EthType.IPv4)
								.setExact(MatchField.IPV4_DST, ip_pkt.getDestinationAddress())
								.build());
						
						actions.add(sw.getOFFactory().actions().output(dstSwitch.getPort(), 0));
						fab.setActions(actions);
						fab.setIdleTimeout(oPolicy.getFlowIdleTimeout());
						fab.setHardTimeout(oPolicy.getFlowHardTimeout());

						fab.setFlags(new HashSet<OFFlowModFlags>(Arrays.asList(OFFlowModFlags.SEND_FLOW_REM)));
						switchService.getSwitch(srcSwitch.getSwitch().getId()).write(fab.build());
						pushPacket(eth, srcSwitch.getSwitch(), pi.getBufferId(), pi.getMatch().get(MatchField.IN_PORT), dstSwitch.getPort());
					}
					else { // src and dst sw are not the same
						boolean firstSwitch = true;
						boolean outgoing = false;
						boolean existingRule = false;
						
						ObfuscatedFlow oFlow = new ObfuscatedFlow();
						oFlow.setSrcMacAddress(eth.getSourceMACAddress());
						oFlow.setDstMacAddress(eth.getDestinationMACAddress());
						oFlow.setSrcIpAddress(ip_pkt.getSourceAddress());
						oFlow.setDstIpAddress(ip_pkt.getDestinationAddress());
						oFlow.setIpProtocol(ip_pkt.getProtocol());

						if (isTCP) {
							oFlow.setSrcPort(((TCP)ip_pkt.getPayload()).getSourcePort());
							oFlow.setDstPort(((TCP)ip_pkt.getPayload()).getDestinationPort());
						}
						else if (isUDP) {
							oFlow.setSrcPort(((UDP)ip_pkt.getPayload()).getSourcePort());
							oFlow.setDstPort(((UDP)ip_pkt.getPayload()).getDestinationPort());
						}

						//oFlow.setObfuscationMask(oMaskManager.getObfuscationMask(eth, sw, route));
						long dst = ObfuscationPolicy.getEndpointIdentifier(false, oFlow.getDstMacAddress().getLong(), oFlow.getDstIpAddress().getInt(), oFlow.getDstPort().getPort());
						oFlow.setObfuscationMask(oMaskManager.getObfuscationMask(dst, sw, route));
						oFlow.assignSrcDstID();
						oFlow.buildObfuscationHeader();
						
						ObfuscationHeader oHeader = oFlow.getObfuscationHeader();
						oHeader.clearMask();
						oHeader.addMask(oFlow.getObfuscationMask().getDstIdMask());
						
						log.debug(oHeader.toString());
						log.debug(oFlow.toString());
						
						List<NodePortTuple> path = route.getPath();
						
						for (NodePortTuple l: path) {
							
							if ((existingRule) && (l.getNodeId() != dstSwitch.getSwitch().getId())) { // to avoid loops for random routes, only need to install a rule at last switch
								continue;
							}
							
							if (!outgoing) {
								Iterator<Link> it = linkDiscoveryService.getPortLinks().get(l).iterator();
								while (it.hasNext()) {
									oLinkStateManager.registerFlow(it.next(), oFlow);
								}
								oSwitchStateManager.registerFlow(switchService.getSwitch(l.getNodeId()), oFlow);
							}
							
							
							
							ArrayList<OFAction> actions = new ArrayList<OFAction>();
							OFFlowAdd.Builder fab = factory.buildFlowAdd();
							fab.setBufferId(OFBufferId.NO_BUFFER);
							fab.setPriority(FlowModUtils.PRIORITY_MAX);
							
							if (firstSwitch) {
								Builder match = factory.buildMatch();
								match.setExact(MatchField.ETH_DST, oFlow.getDstMacAddress());
								match.setExact(MatchField.ETH_SRC, oFlow.getSrcMacAddress());
								match.setExact(MatchField.ETH_TYPE, EthType.IPv4);
								match.setExact(MatchField.IP_PROTO, oFlow.getIpProtocol());
								match.setExact(MatchField.IPV4_SRC, oFlow.getSrcIpAddress());
								match.setExact(MatchField.IPV4_DST, oFlow.getDstIpAddress());
								
								if (isTCP) {
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC) && oPolicy.doRewrite(oFlow.getSrcPort().getPort()))
										match.setExact(MatchField.TCP_SRC, oFlow.getSrcPort());
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST) && oPolicy.doRewrite(oFlow.getDstPort().getPort()))
										match.setExact(MatchField.TCP_DST, oFlow.getDstPort());
								}
								else if (isUDP) {
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC) && oPolicy.doRewrite(oFlow.getSrcPort().getPort()))
										match.setExact(MatchField.UDP_SRC, oFlow.getSrcPort());
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST) && oPolicy.doRewrite(oFlow.getDstPort().getPort()))
										match.setExact(MatchField.UDP_DST, oFlow.getDstPort());
								}
								
								fab.setMatch(match.build());
								
								if (oPolicy.doRewrite(ObfuscationPolicy.Field.IP_SRC))
									actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ipv4Src(oHeader.getObfuscatedSrcIpValue())));
								if (oPolicy.doRewrite(ObfuscationPolicy.Field.IP_DST))
									actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ipv4Dst(oHeader.getObfuscatedDstIpValue())));
								if (oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_SRC))
									actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ethSrc(oHeader.getObfuscatedSrcMacValue())));
								if (oPolicy.doRewrite(ObfuscationPolicy.Field.MAC_DST))
									actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ethDst(oHeader.getObfuscatedDstMacValue())));

								if (isTCP) {
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC) && oPolicy.doRewrite(oFlow.getSrcPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().tcpSrc(oHeader.getObfuscatedSrcPort())));
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST) && oPolicy.doRewrite(oFlow.getDstPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().tcpDst(oHeader.getObfuscatedDstPort())));
								}
								else if (isUDP) {
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC) && oPolicy.doRewrite(oFlow.getSrcPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().udpSrc(oHeader.getObfuscatedSrcPort())));
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST) && oPolicy.doRewrite(oFlow.getDstPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().udpDst(oHeader.getObfuscatedDstPort())));
								}
								
								actions.add(sw.getOFFactory().actions().output(l.getPortId(), 0));
							}
							else if (l.getNodeId() == dstSwitch.getSwitch().getId()) { //last switch
								oHeader.addMask(oFlow.getObfuscationMask().getSrcIdMask());
								oHeader.addMask(oFlow.getObfuscationMask().getDstIdMask());
								

								fab.setMatch(factory
										.buildMatch()
										.setMasked(MatchField.ETH_DST, oHeader.getObfuscatedDstMacValue(), oHeader.getObfuscatedDstMacMask())
										.setMasked(MatchField.ETH_SRC, oHeader.getObfuscatedSrcMacValue(), oHeader.getObfuscatedSrcMacMask())
										.setExact(MatchField.ETH_TYPE, EthType.IPv4)
										.setExact(MatchField.IP_PROTO, oFlow.getIpProtocol())
										.setMasked(MatchField.IPV4_SRC, oHeader.getObfuscatedSrcIpValue(), oHeader.getObfuscatedSrcIpMask())
										.setMasked(MatchField.IPV4_DST, oHeader.getObfuscatedDstIpValue(), oHeader.getObfuscatedDstIpMask())
										.build());

								actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ipv4Src(oFlow.getSrcIpAddress())));
								actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ipv4Dst(oFlow.getDstIpAddress())));
								actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ethSrc(oFlow.getSrcMacAddress())));
								actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().ethDst(oFlow.getDstMacAddress())));
								
								if (isTCP) {
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC) && oPolicy.doRewrite(oFlow.getSrcPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().tcpSrc(oFlow.getSrcPort())));
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST) && oPolicy.doRewrite(oFlow.getDstPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().tcpDst(oFlow.getDstPort())));
								}
								else if (isUDP) {
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_SRC) && oPolicy.doRewrite(oFlow.getSrcPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().udpSrc(oFlow.getSrcPort())));
									if (oPolicy.doRewrite(ObfuscationPolicy.Field.TP_DST) && oPolicy.doRewrite(oFlow.getDstPort().getPort()))
										actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().udpDst(oFlow.getDstPort())));
								}
								
								actions.add(sw.getOFFactory().actions().output(dstSwitch.getPort(), 0));
							}
							else if (!outgoing) {
								fab.setCookie(U64.of(dst));
								fab.setMatch(factory
										.buildMatch()
										.setMasked(MatchField.ETH_DST, oHeader.getObfuscatedDstMacValue(), oHeader.getObfuscatedDstMacMask())
										.setMasked(MatchField.ETH_SRC, oHeader.getObfuscatedSrcMacValue(), oHeader.getObfuscatedSrcMacMask())
										.setExact(MatchField.ETH_TYPE, EthType.IPv4)
										.setExact(MatchField.IP_PROTO, oFlow.getIpProtocol())
										.setMasked(MatchField.IPV4_SRC, oHeader.getObfuscatedSrcIpValue(), oHeader.getObfuscatedSrcIpMask())
										.setMasked(MatchField.IPV4_DST, oHeader.getObfuscatedDstIpValue(), oHeader.getObfuscatedDstIpMask())
										.build());
								
								actions.add(sw.getOFFactory().actions().output(l.getPortId(), 0));
							}
							
							if (!actions.isEmpty()) {
								fab.setActions(actions);

								fab.setIdleTimeout(oPolicy.getFlowIdleTimeout());

								if (firstSwitch)
									fab.setHardTimeout(oPolicy.getFlowHardTimeout());
								else
									fab.setHardTimeout(0);
								
								fab.setFlags(new HashSet<OFFlowModFlags>(Arrays.asList(OFFlowModFlags.SEND_FLOW_REM)));
								
								
								
								
								if ((!oSwitchStateManager.checkDestinationID(switchService.getSwitch(l.getNodeId()), oFlow.getObfuscationMask().getDst())) | true /* TODO only debug*/) {
									switchService.getSwitch(l.getNodeId()).write(fab.build());
									
									//if ((!firstSwitch) && !(l.getNodeId() == dstSwitch.getSwitch().getId())) // not at first switch and last switch
									//	oSwitchStateManager.registerDestinationID(switchService.getSwitch(l.getNodeId()), oFlow.getObfuscationMask().getDst(), path);
								}
								else {
									existingRule = true;
									log.debug("rule already installed at switch {}",l.getNodeId());
									
									List<NodePortTuple> existingPath = oSwitchStateManager.getPathForDestinationID(switchService.getSwitch(l.getNodeId()), oFlow.getObfuscationMask().getDst());

									boolean outgoing_ex = false;
									boolean swPassed = false;
									for (NodePortTuple l_ex: existingPath) {
										if (l_ex.getNodeId().equals(l.getNodeId())){
											swPassed = true;
											continue;
										}
										
										if ((!outgoing_ex) & swPassed) {
											Iterator<Link> it_ex = linkDiscoveryService.getPortLinks().get(l_ex).iterator();
											while (it_ex.hasNext()) {
												oLinkStateManager.registerFlow(it_ex.next(), oFlow);
											}
											oSwitchStateManager.registerFlow(switchService.getSwitch(l_ex.getNodeId()), oFlow);
										}
										outgoing_ex = !outgoing_ex;
									}
								}
							}
							if (firstSwitch)
								firstSwitch = false;
							outgoing = !outgoing;
						}
						//pushPacket(eth, dstSwitch.getSwitch(), OFBufferId.NO_BUFFER, pi.getMatch().get(MatchField.IN_PORT), dstSwitch.getPort());
						pushPacket(eth, dstSwitch.getSwitch(), OFBufferId.NO_BUFFER, OFPort.ANY, dstSwitch.getPort());
					}
				}
			}
			else {
				dstSwitch = srcSwitch;
				log.warn("IpToSwitch does not contain {}",ip_pkt.getDestinationAddress());
			}
		}
		else if (packetBelongsToFlow(sw, pi, cntx)) { // packet belongs to obfuscated flow -> restore fwd rule
			log.warn("packet belongs to obfuscated flow -> restore fwd rule  for packet {}",ip_pkt);
			
			ObfuscatedFlow flowFromPacket = oSwitchStateManager.getFlowFromPacket(sw, eth);			
			List<NodePortTuple> flowFromPacketPath = oSwitchStateManager.getPathForDestinationID(sw, flowFromPacket.getObfuscationMask().getDst());
			
			boolean found = false;
			
			for (NodePortTuple l: flowFromPacketPath) {
				if (found)
					pushPacket(eth, sw, OFBufferId.NO_BUFFER, OFPort.ANY, l.getPortId());
				if (l.getNodeId().equals(sw.getId()))
					found = true;
			}
		}
		else { // suspicious packet -> raise alert
			log.warn("suspicious packet  {}",ip_pkt);
		}
	}
	
	/**
	 * checks if the packet arrived at a host-facing port
	 * @param sw
	 * @param pi
	 * @param cntx
	 * @return
	 */
	private boolean packetFromHost(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		for (Link link : linkDiscoveryService.getSwitchLinks().get(sw.getId())) {
			if ((link.getSrc() == sw.getId()) && (link.getSrcPort() == pi.getMatch().get(MatchField.IN_PORT))) { // link where the packet arrived
				if (switchService.getActiveSwitch(link.getDst()) != null) // endpoint of link is another switch -> not a host
					return false;
			}
        }
		return true;
	}
	 
	/**
	 * checks if the packet belongs to a flow at this switch. If yes, this means that the packet is obfuscated and came to the controller for example because a switch crashed.
	 * @param sw
	 * @param pi
	 * @param cntx
	 * @return
	 */
	private boolean packetBelongsToFlow(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		log.debug("packet belongs to flow?");
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		return oSwitchStateManager.checkFlowFromPacket(sw, eth);
	}
	

	private void handleARP(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		if (! (eth.getPayload() instanceof ARP)) // not an ARP packet
			return;
		
		ARP arpRequest = (ARP) eth.getPayload();
		
		if (arpRequest.getOpCode() == ARP.OP_REPLY) { // is a reply
			oTopologyManager.updateTopologyMappings(sw, pi, cntx);
			
			for (ARP pendingArpRequest : arpRequestBuffer.getPendingRequests(IPv4Address.of(arpRequest.getSenderProtocolAddress()))) {				
				if (oTopologyManager.knowSwitchForIp(IPv4Address.of(pendingArpRequest.getSenderProtocolAddress()))) {
					SwitchHostInfo dstSwitchPort = oTopologyManager.getSwitchForIp(IPv4Address.of(pendingArpRequest.getSenderProtocolAddress()));
					sendArpReply(MacAddress.of(arpRequest.getSenderHardwareAddress()), IPv4Address.of(arpRequest.getSenderProtocolAddress()), MacAddress.of(pendingArpRequest.getSenderHardwareAddress()), IPv4Address.of(pendingArpRequest.getSenderProtocolAddress()), dstSwitchPort.getSwitch(), dstSwitchPort.getPort());
					arpRequestBuffer.removeRequest(pendingArpRequest);
				}
				else
					log.warn("answering pending ARP request failed because dst switch/port is not known. {}",pendingArpRequest);
			}
		}
		else { // is a request
			if (IPv4Address.of(arpRequest.getSenderProtocolAddress()).toString().contentEquals("10.0.0.111")) // ignore crafted requests from switches
				return;
			
			if (oTopologyManager.knowMacForIp(IPv4Address.of(arpRequest.getTargetProtocolAddress()))) {
				MacAddress senderMac = oTopologyManager.getMacForIp(IPv4Address.of(arpRequest.getTargetProtocolAddress()));
				sendArpReply(senderMac, IPv4Address.of(arpRequest.getTargetProtocolAddress()), MacAddress.of(arpRequest.getSenderHardwareAddress()), IPv4Address.of(arpRequest.getSenderProtocolAddress()), sw, pi.getMatch().get(MatchField.IN_PORT));
			}
			else {
				arpRequestBuffer.addRequest(arpRequest);
				for (DatapathId swi : switchService.getAllSwitchDpids())
					floodArpRequest(switchService.getSwitch(swi),IPv4Address.of(arpRequest.getTargetProtocolAddress()));
			}
		}
	}
	
	private void handleCbench(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		//log.debug("Cbench PacketIn {}",eth);
	
		
		OFFactory factory = switchService.getSwitch(sw.getId()).getOFFactory();
		
		
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFFlowAdd.Builder fab = factory.buildFlowAdd();
		fab.setBufferId(OFBufferId.NO_BUFFER);
		fab.setPriority(FlowModUtils.PRIORITY_MAX);
		
		fab.setMatch(factory
				.buildMatch()
				.setExact(MatchField.ETH_DST, eth.getDestinationMACAddress())
				.build());
		
		//actions.add(sw.getOFFactory().actions().output(sw.getPort(portNumber), 0));
		fab.setActions(actions);
		sw.write(fab.build());
			
	}
	
	private void sendArpReply(MacAddress senderMac, IPv4Address senderIp, MacAddress targetMac, IPv4Address targetIp, IOFSwitch sw, OFPort port) {
		IPacket arpReply = new Ethernet()
			.setSourceMACAddress(senderMac)
			.setDestinationMACAddress(targetMac)
			.setEtherType(EthType.ARP)
			.setPayload(
				new ARP()
				.setHardwareType(ARP.HW_TYPE_ETHERNET)
				.setProtocolType(ARP.PROTO_TYPE_IP)
				.setHardwareAddressLength((byte) 6)
				.setProtocolAddressLength((byte) 4)
				.setOpCode(ARP.OP_REPLY)
				.setSenderHardwareAddress(senderMac.getBytes())
				.setSenderProtocolAddress(senderIp.getBytes())
				.setTargetHardwareAddress(targetMac.getBytes())
				.setTargetProtocolAddress(targetIp.getBytes()));
        pushPacket(arpReply, sw, OFBufferId.NO_BUFFER, OFPort.ANY, port);
	}
	
	
	private void floodArpRequest(IOFSwitch sw, IPv4Address requestedAddress) {
		IPacket arpRequest = new Ethernet()
		.setSourceMACAddress("00:00:00:00:00:01")
		.setDestinationMACAddress("ff:ff:ff:ff:ff:ff")
		.setEtherType(EthType.ARP)
		.setVlanID((short) 0)
		.setPriorityCode((byte) 0)
		.setPayload(
				new ARP()
				.setHardwareType(ARP.HW_TYPE_ETHERNET)
				.setProtocolType(ARP.PROTO_TYPE_IP)
				.setHardwareAddressLength((byte) 6)
				.setProtocolAddressLength((byte) 4)
				.setOpCode(ARP.OP_REQUEST)
				.setSenderHardwareAddress(HexString.fromHexString("00:00:00:00:00:01"))
				.setSenderProtocolAddress(IPv4.toIPv4AddressBytes("10.0.0.111"))
				.setTargetHardwareAddress(HexString.fromHexString("00:00:00:00:00:00"))
				.setTargetProtocolAddress(requestedAddress.getBytes()));
        
        Set<OFPort> portsConnectedToSwitches = new HashSet<OFPort>();
        
        for (Link link : linkDiscoveryService.getSwitchLinks().get(sw.getId())) {
        	if (link.getSrc() == sw.getId())
        		portsConnectedToSwitches.add(link.getSrcPort());
        }
        
        for (OFPortDesc port : sw.getPorts()) {
        	if (!portsConnectedToSwitches.contains(port.getPortNo())) {
        		pushPacket(arpRequest, sw, OFBufferId.NO_BUFFER, OFPort.ANY, port.getPortNo());
        	}
        }
	}
	
	/**
	 * used to push any packet - borrowed routine from Forwarding
	 * 
	 * @param OFPacketIn pi
	 * @param IOFSwitch sw
	 * @param int bufferId
	 * @param short inPort
	 * @param short outPort
	 */    
	public void pushPacket(IPacket packet, 
	                       IOFSwitch sw,
	                       OFBufferId bufferId,
	                       OFPort inPort,
	                       OFPort outPort
	                       ) {
		pushPacket(packet,sw,bufferId,inPort,outPort,new ArrayList<OFAction>());
	}
	
	/**
	 * used to push any packet - borrowed routine from Forwarding
	 * 
	 * @param OFPacketIn pi
	 * @param IOFSwitch sw
	 * @param int bufferId
	 * @param short inPort
	 * @param short outPort
	 * @param List<OFAction> actions
	 */    
	public void pushPacket(IPacket packet, 
	                       IOFSwitch sw,
	                       OFBufferId bufferId,
	                       OFPort inPort,
	                       OFPort outPort,
	                       List<OFAction> actions
	                       ) {
	        log.trace("PacketOut srcSwitch={} inPort={} outPort={}", new Object[] {sw, inPort, outPort});
	
	    OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
	
	    // set actions
	    //List<OFAction> actions = new ArrayList<OFAction>();
	    actions.add(sw.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(Integer.MAX_VALUE).build());
	
	    pob.setActions(actions);
	    
	    // set buffer_id, in_port
	    pob.setBufferId(bufferId);
	    pob.setInPort(inPort);
	
	    // set data - only if buffer_id == -1
	    if (pob.getBufferId() == OFBufferId.NO_BUFFER) {
	        if (packet == null) {
	            log.error("BufferId is not set and packet data is null. " +
	                      "Cannot send packetOut. " +
	                    "srcSwitch={} inPort={} outPort={}",
	                    new Object[] {sw, inPort, outPort});
	            return;
	        }
	        byte[] packetData = packet.serialize();
	        pob.setData(packetData);
	    }
	
	    //counterPacketOut.increment();
	    sw.write(pob.build());
	}
	

	@Override
	public String getName() {
		return ObfuscationController.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IObfuscationController.class);
	    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(IObfuscationController.class, this);
	    return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IObfuscationSwitchStateManager.class);
		l.add(IObfuscationLinkStateManager.class);
		l.add(IObfuscationRoutingService.class);
		l.add(IObfuscationMaskManager.class);
		l.add(ILinkDiscoveryService.class);
		l.add(IOFSwitchService.class);
		return l;
	}
}
