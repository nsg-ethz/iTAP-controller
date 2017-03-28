package ch.ethz.tik.sdnobfuscation;

import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ObfuscationRestApiResourceMasks extends ServerResource {
    @Get("json")
    public Map<Long, ObfuscationMask> retrieve() {
        IObfuscationMaskManager oMaskManager = (IObfuscationMaskManager)getContext().getAttributes().get(IObfuscationMaskManager.class.getCanonicalName());
        return oMaskManager.getObfuscationMasks();
    }
}
