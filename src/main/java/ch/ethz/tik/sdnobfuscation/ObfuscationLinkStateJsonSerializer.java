package ch.ethz.tik.sdnobfuscation;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ObfuscationLinkStateJsonSerializer extends
		JsonSerializer<ObfuscationLinkState> {

    @Override
    public void serialize(ObfuscationLinkState oLinkState, JsonGenerator jGen,
                          SerializerProvider arg2) throws IOException,
                                                  JsonProcessingException {
        jGen.writeStartObject();

        jGen.writeStringField("src", oLinkState.getLink().getSrc().toString());
        jGen.writeStringField("dst", oLinkState.getLink().getDst().toString());
        jGen.writeNumberField("srcPort", oLinkState.getLink().getSrcPort().getPortNumber());
        jGen.writeNumberField("dstPort", oLinkState.getLink().getDstPort().getPortNumber());
        
        jGen.writeFieldName("masks");
        jGen.writeStartArray();
        for (long dst: oLinkState.getInstalledMasks().keySet()) {
            jGen.writeStartObject();
        	//jGen.writeStringField("dst", dst.toString());
        	jGen.writeNumberField("observed_entropy", oLinkState.getObservedEntropy(dst));
        	jGen.writeNumberField("unicity_distance", oLinkState.getUnicityDistance(dst));
        	jGen.writeNumberField("flows", oLinkState.getNumberOfMaskUsages(dst));
        	jGen.writeEndObject();
        }
        jGen.writeEndArray();
        
        
        jGen.writeEndObject();
    }

}
