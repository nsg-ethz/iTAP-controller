package ch.ethz.tik.sdnobfuscation;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class ObfuscationRestApiWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/masks/json", ObfuscationRestApiResourceMasks.class);
        router.attach("/links/json", ObfuscationRestApiResourceLinks.class);
        //router.attach("/tm/json", ObfuscationRestApiResourceTrafficMatrix.class);
        return router;
    }
 
    @Override
    public String basePath() {
        return "/wm/obfuscation";
    }

}
