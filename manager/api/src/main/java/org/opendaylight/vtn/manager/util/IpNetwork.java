/*
 * Copyright (c) 2015 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.util;

import java.io.Serializable;
import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;

/**
 * {@code IpNetwork} describes an IP network.
 * IP network is specified by an IP address and prefix length.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement(name = "ip-network")
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso(Ip4Network.class)
public abstract class IpNetwork implements Serializable {
    /**
     * Version number for serialization.
     */
    private static final long serialVersionUID = -2052424244109273830L;

    /**
     * A character that separates prefix length from IP address in CIDR
     * notation.
     */
    public static final char  CIDR_SEPARATOR = '/';

    /**
     * An {@link InetAddress} instance which represents an IP network
     * address.
     */
    private InetAddress  inetAddress;

    /**
     * Prefix length that specifies network range.
     */
    private int  prefixLength;

    /**
     * Create an {@link InetAddress} instance which represents the IP address
     * specified by the given byte array.
     *
     * @param bytes  A byte array which represents the IP address.
     * @return  An {@link InetAddress} instance.
     * @throws IllegalArgumentException
     *    The given byte array does not represent an IP network mask.
     */
    public static final InetAddress getInetAddress(byte[] bytes) {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid raw IP address: " + ByteUtils.toHexString(bytes));
        }
    }

    /**
     * Create an {@link InetAddress} instance which represents the IP address
     * specified by the given string.
     *
     * @param text  A string representation of an IP address.
     * @return  An {@link InetAddress} instance.
     * @throws NullPointerException
     *    {@code text} is {@code null}.
     * @throws IllegalArgumentException
     *    The given string does not represent an IP address.
     */
    public static final InetAddress getInetAddress(String text) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be empty.");
        }
        try {
            return InetAddress.getByName(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address: " + text);
        }
    }

    /**
     * Private constructor used for JAXB mapping.
     */
    @SuppressWarnings("unused")
    IpNetwork() {
    }

    /**
     * Construct a new instance.
     *
     * @param prefix  Prefix length that specifies network range.
     *                Note that zero means "no mask". So zero is treated as if
     *                the maximum prefix length is specified.
     */
    IpNetwork(int prefix) {
        prefixLength = (prefix == 0) ? getMaxPrefix() : prefix;
    }

    /**
     * Construct a new instance.
     *
     * @param iaddr   An {@link InetAddress} instance which representse the
     *                network address.
     * @param prefix  Prefix length that specifies network range.
     *                Note that zero means "no mask". So zero is treated as if
     *                the maximum prefix length is specified.
     * @throws NullPointerException
     *    {@code iaddr} is {@code null}.
     * @throws IllegalArgumentException
     *    The given network address or prefix length is invalid.
     */
    IpNetwork(InetAddress iaddr, int prefix) {
        int plen = (prefix == 0) ? getMaxPrefix() : prefix;
        inetAddress = init(iaddr, plen);
        prefixLength = plen;
    }

    /**
     * Construct a new instance.
     *
     * @param cidr  A string representation of the IP network in CIDR notation.
     *              Note that zero prefix means "no mask". So zero prefix is
     *              treated as if the maximum prefix length is specified.
     * @throws NullPointerException
     *    {@code cidr} is {@code null}.
     * @throws IllegalArgumentException
     *    The given network address is invalid.
     */
    IpNetwork(String cidr) {
        setCidrValue(cidr);
    }

    /**
     * Return the length of network prefix.
     *
     * @return  The prefix length.
     */
    public final int getPrefixLength() {
        return prefixLength;
    }

    /**
     * Return an {@link InetAddress} instance which represents the IP network
     * address.
     *
     * @return  An {@link InetAddress} instance.
     */
    public final InetAddress getInetAddress() {
        InetAddress iaddr = inetAddress;
        if (iaddr == null) {
            iaddr = getInetAddress(getBytes());
            inetAddress = iaddr;
        }

        return iaddr;
    }

    /**
     * Return a string representation of IP network.
     *
     * <p>
     *   If this instance represents a specific IP host, an IP address of the
     *   host without prefix length is returned. Otherwise a string which
     *   represents the IP network in CIDR notation is returned.
     * </p>
     *
     * @return  A string representation of IP network.
     */
    public final String getText() {
        int max = getMaxPrefix();
        return (prefixLength == max) ? getHostAddress() : getCidrText();
    }

    /**
     * Return the maximum length of the network prefix.
     *
     * @return  The maximum length of the network prefix.
     */
    public abstract int getMaxPrefix();

    /**
     * Return an {@link IpPrefix} instance which represents this instance.
     *
     * @return  An {@link IpPrefix} instance.
     */
    public abstract IpPrefix getIpPrefix();

    /**
     * Return a string representation of this network in CIDR notation.
     *
     * @return  A string representation of this network in CIDR notation.
     */
    public abstract String getCidrText();

    /**
     * Return a string representation of this network address without
     * CIDR prefix.
     *
     * @return  A string representation of this network address.
     */
    public abstract String getHostAddress();

    /**
     * Return a byte array which represents the raw network address.
     *
     * @return  A byte array which represents the raw network address.
     */
    public abstract byte[] getBytes();

    /**
     * Determine whether this network contains the given IP host.
     *
     * @param inw  An {@code IpNetwork} instance to be tested.
     *             Note that this method assumes that this argument specifies
     *             the IP host, not IP network. So the CIDR prefix length
     *             configured in this argument is always ignored.
     * @return  {@code true} only if the IP network corresponding to this
     *          instance contains the IP network specified by {@code inw}.
     * @throws NullPointerException
     *    {@code inw} is {@code null}.
     * @throws IllegalArgumentException
     *    This instance cannot test the given network.
     */
    public abstract boolean contains(IpNetwork inw);

    /**
     * Initialize this instance.
     *
     * @param iaddr   An {@link InetAddress} instance which represents the
     *                network address.
     * @param prefix  The prefix length.
     * @return  An {@link InetAddress} instance which represents the IP network
     *          address specified by the given pair of IP address and prefix
     *          length.
     * @throws NullPointerException
     *    {@code ipaddr} is {@code null}.
     * @throws IllegalArgumentException
     *    The given IP address or prefix length is invalid.
     */
    abstract InetAddress init(InetAddress iaddr, int prefix);

    // JAXB methods

    /**
     * Return a string representation of this network address in CIDR notation.
     *
     * @return  A string representation of this network address.
     * @deprecated  Only for JAXB. Use {@link #getCidrText()} instead.
     */
    @XmlValue
    public final String getCidrValue() {
        return getCidrText();
    }

    /**
     * Set the IP network address by a string representation of network address
     * in CIDR notation.
     *
     * @param cidr  A string representation of this network address.
     * @throws NullPointerException
     *    {@code iaddr} is {@code null}.
     * @throws IllegalArgumentException
     *    The given network address or prefix length is invalid.
     */
    private void setCidrValue(String cidr) {
        String text = cidr.trim();
        int pos = text.indexOf((int)CIDR_SEPARATOR);
        String host;
        int prefix;
        if (pos < 0) {
            host = text;
            prefix = getMaxPrefix();
        } else {
            host = text.substring(0, pos);
            try {
                String pstr = text.substring(pos + 1, text.length());
                prefix = Integer.parseInt(pstr);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                    "Invalid CIDR prefix: " + text, e);
            }
            if (prefix == 0) {
                prefix = getMaxPrefix();
            }
        }

        InetAddress iaddr = getInetAddress(host);
        inetAddress = init(iaddr, prefix);
        prefixLength = prefix;
    }

    // Object

    /**
     * Return a string representation of this object.
     *
     * @return  A string representation of this object.
     */
    @Override
    public final String toString() {
        String name = getClass().getSimpleName();
        StringBuilder builder = new StringBuilder(name);
        return builder.append('[').append(getText()).append(']').toString();
    }
}
