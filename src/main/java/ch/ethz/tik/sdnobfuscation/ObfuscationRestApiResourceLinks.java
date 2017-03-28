package ch.ethz.tik.sdnobfuscation;

import java.util.Map;

import net.floodlightcontroller.routing.Link;

import org.restlet.resource.Get; 
import org.restlet.resource.ServerResource;

public class ObfuscationRestApiResourceLinks extends ServerResource {
    @Get("json")
    public Map<Link, ObfuscationLinkState> retrieve() {
        IObfuscationLinkStateManager oLinkStateManager = (IObfuscationLinkStateManager)getContext().getAttributes().get(IObfuscationLinkStateManager.class.getCanonicalName());
        return oLinkStateManager.getLinkStates();
    }
}
