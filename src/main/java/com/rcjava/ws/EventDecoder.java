package com.rcjava.ws;

import com.google.protobuf.InvalidProtocolBufferException;
import com.rcjava.protos.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.nio.ByteBuffer;

/**
 * @author zyf
 */
public class EventDecoder implements Decoder.Binary<Peer.Event> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Peer.Event decode(ByteBuffer bytes) throws DecodeException {
        try {
            return Peer.Event.parseFrom(bytes);
        } catch (InvalidProtocolBufferException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public boolean willDecode(ByteBuffer bytes) {
        try {
            Peer.Event.parseFrom(bytes);
        } catch (InvalidProtocolBufferException ex) {
            return false;
        }
        return true;
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
