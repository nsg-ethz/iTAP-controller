package ch.ethz.tik.sdnobfuscation;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ObfuscationMaskJsonSerializer extends
		JsonSerializer<ObfuscationMask> {

    @Override
    public void serialize(ObfuscationMask oMask, JsonGenerator jGen,
                          SerializerProvider arg2) throws IOException,
                                                  JsonProcessingException {
        jGen.writeStartObject();
        //jGen.writeStringField("dst", oMask.getDst().toString());
        jGen.writeStringField("srcIdMask", oMask.getSrcIdMask().toString());
        jGen.writeStringField("dstIdMask", oMask.getDstIdMask().toString());
        jGen.writeNumberField("srcIdsUsed", oMask.getSourceIDsSize());
        jGen.writeNumberField("dstIdsUsed", oMask.getDestinationIDsSize());
        jGen.writeEndObject();
    }

}
