/* 
 * Copyright (C) 2015 SDN-WISE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.sdnwiselab.sdnwise.packet;

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.REG_PROXY;
import com.github.sdnwiselab.sdnwise.util.*;
import static com.github.sdnwiselab.sdnwise.util.NodeAddress.BROADCAST_ADDR;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.*;

/**
 *
 * @author Sebastiano Milardo
 */
public class RegProxyPacket extends NetworkPacket {

    private static final Logger LOGGER = Logger.getLogger(RegProxyPacket.class.getName());

    private final static int D_PID_LEN = 8;
    private final static int MAC_LEN = 6;
    private final static int PORT_LEN = 8;
    private final static int IP_LEN = 4;

    public final static int SDN_WISE_DPID = 0,
            SDN_WISE_MAC = SDN_WISE_DPID + D_PID_LEN,
            SDN_WISE_PORT = SDN_WISE_MAC + MAC_LEN,
            SDN_WISE_IP = SDN_WISE_PORT + PORT_LEN,
            SDN_WISE_TCP = SDN_WISE_IP + IP_LEN;

    /**
     * This constructor initialize a beacon packet starting from a byte array.
     *
     * @param data the byte array representing the beacon packet.
     */
    public RegProxyPacket(byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize a beacon packet starting from a
     * NetworkPacket.
     *
     * @param data the NetworkPacket representing the beacon packet.
     */
    public RegProxyPacket(NetworkPacket data) {
        super(data.toByteArray());
    }

    /**
     * This constructor initialize a beacon packet. The type of the packet is
     * set to SDN_WISE_BEACON and the destination address is BROADCAST_ADDR.
     *
     * @param netId
     * @param src
     * @param switchDpid
     * @param switchMac
     * @param switchPort
     * @param inetAddr
     */
    public RegProxyPacket(int netId, NodeAddress src,
            String switchDpid,
            String switchMac,
            long switchPort,
            InetSocketAddress inetAddr) {
        super(netId, src, BROADCAST_ADDR);
        setTyp(REG_PROXY);
        setSwitchMac(switchMac);
        setSwitchDpid(switchDpid);
        setSwitchPort(switchPort);
        setSrc(src);
        setDst(src);
        setNxh(src);
        setInetSocketAddress(inetAddr);
    }

    /**
     * This constructor initialize a beacon packet starting from a int array.
     *
     * @param data the int array representing the beacon packet, all int are
     * casted to byte.
     */
    public RegProxyPacket(int[] data) {
        super(data);
    }

    public final RegProxyPacket setSwitchMac(String mac) {
        String[] elements = mac.split(":");
        if (elements.length != MAC_LEN) {
            throw new IllegalArgumentException("Invalid MAC address");
        }
        for (int i = 0; i < MAC_LEN; i++) {
            this.setPayloadAt((byte) Integer.parseInt(elements[i], 16),
                    SDN_WISE_MAC + i);
        }
        return this;
    }

    public final String getSwitchMac() {
        StringBuilder sb = new StringBuilder(18);
        byte[] mac = this.getPayloadFromTo(SDN_WISE_MAC, SDN_WISE_MAC + MAC_LEN);
        for (byte b : mac) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public final RegProxyPacket setSwitchDpid(String ofSwitchId) {
        byte[] dpid = ofSwitchId.getBytes(Charset.forName("UTF-8"));
        int len = Math.min(D_PID_LEN, dpid.length);
        this.setPayload(dpid, 0, SDN_WISE_DPID, len);
        return this;
    }

    public final String getSwitchDpid() {
        return new String(this.getPayloadFromTo(SDN_WISE_DPID, SDN_WISE_MAC));
    }

    public final RegProxyPacket setSwitchPort(long port) {
        byte[] bytes = ByteBuffer
                .allocate(Long.SIZE / Byte.SIZE).putLong(port).array();
        this.setPayload(bytes, (byte) 0, SDN_WISE_PORT, PORT_LEN);
        return this;
    }

    public final long getSwitchPort() {
        return new BigInteger(this.getPayloadFromTo(SDN_WISE_PORT, SDN_WISE_PORT + PORT_LEN)).longValue();
    }

    public final RegProxyPacket setInetSocketAddress(InetSocketAddress isa) {
        byte[] ip = isa.getAddress().getAddress();
        int port = isa.getPort();
        return setInetSocketAddress(ip, port);
    }

    private RegProxyPacket setInetSocketAddress(byte[] ip, int port) {
        this.setPayload(ip, 0, SDN_WISE_IP, IP_LEN);
        this.setPayloadAt((byte) (port), SDN_WISE_TCP + 1);
        this.setPayloadAt((byte) (port >> 8), SDN_WISE_TCP);
        return this;
    }

    public final InetSocketAddress getInetSocketAddress() {
        try {
            byte[] ip = this.getPayloadFromTo(SDN_WISE_IP, SDN_WISE_IP + IP_LEN);
            return new InetSocketAddress(InetAddress.getByAddress(ip),
                    Utils.mergeBytes(getPayloadAt(SDN_WISE_TCP), getPayloadAt(SDN_WISE_TCP + 1)));
        } catch (UnknownHostException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return null;
    }
}