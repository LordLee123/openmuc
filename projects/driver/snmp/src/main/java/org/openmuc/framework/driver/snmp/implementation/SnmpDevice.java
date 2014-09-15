/*
 * Copyright 2011-14 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.snmp.implementation;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DeviceConnection;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.*;

/**
 * Super class for defining SNMP enabled devices.
 *
 * @author Mehran Shakeri
 */
public abstract class SnmpDevice implements DeviceConnection {

    public enum SNMPVersion {
        V1, V2c, V3
    }

    ;

    protected Address targetAddress;
    protected Snmp snmp;
    protected USM usm;
    protected int timeout = 3000; // in milliseconds
    protected int retries = 3;
    protected String authenticationPassphrase;
    protected AbstractTarget target;

    protected List<SnmpDiscoveryListener> listeners = new ArrayList<SnmpDiscoveryListener>();

    public static final Map<String, String> ScanOIDs = new HashMap<String, String>();

    static {
        // some general OIDs that are valid in almost every MIB
        ScanOIDs.put("Device name: ", "1.3.6.1.2.1.1.5.0");
        ScanOIDs.put("Description: ", "1.3.6.1.2.1.1.1.0");
        ScanOIDs.put("Location: ", "1.3.6.1.2.1.1.6.0");
    }

    ;

    /**
     * snmp constructor takes primary parameters in order to create snmp object. this implementation uses UDP protocol
     *
     * @param address                  Contains ip and port. accepted string "X.X.X.X/portNo"
     * @param authenticationPassphrase the authentication pass phrase. If not <code>null</code>, <code>authenticationProtocol</code> must
     *                                 also be not <code>null</code>. RFC3414 §11.2 requires pass phrases to have a minimum length of 8
     *                                 bytes. If the length of <code>authenticationPassphrase</code> is less than 8 bytes an
     *                                 <code>IllegalArgumentException</code> is thrown. [required by snmp4j library]
     * @throws ConnectionException
     * @throws ArgumentSyntaxException
     */
    public SnmpDevice(String address, String authenticationPassphrase) throws ConnectionException,
            ArgumentSyntaxException {

        // start snmp compatible with all versions
        try {
            snmp = new Snmp(new DefaultUdpTransportMapping());
        }
        catch (IOException e) {
            throw new ConnectionException("SNMP initialization failed! \n" + e.getMessage());
        }
        usm = new USM(SecurityProtocols.getInstance(),
                      new OctetString(MPv3.createLocalEngineID()),
                      0);
        SecurityModels.getInstance().addSecurityModel(usm);
        try {
            snmp.listen();
        }
        catch (IOException e) {
            throw new ConnectionException("SNMP listen failed! \n" + e.getMessage());
        }

        // set address
        try {
            targetAddress = GenericAddress.parse(address);
        }
        catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException("Device address foramt is wrong! (eg. 1.1.1.1/1)");
        }

        this.authenticationPassphrase = authenticationPassphrase;

    }

    /**
     * Default constructor useful for scanner
     */
    public SnmpDevice() {
    }

    /**
     * set target parameters. Implementations are different in SNMP v1, v2c and v3
     */
    abstract void setTarget();

    /**
     * Receives a list of all OIDs in string format, creates PDU and sends GET request to defined target. This method is
     * a blocking method. It waits for response.
     *
     * @param OIDs list of OIDs that should be read from target
     * @return Map<String, String> returns a Map of OID as Key and received value corresponding to that OID from the
     * target as Value
     * @throws SnmpTimeoutException
     * @throws ConnectionException
     */
    public Map<String, String> getRequestsList(List<String> OIDs)
            throws SnmpTimeoutException, ConnectionException {

        Map<String, String> result = new HashMap<String, String>();

        // set PDU
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);

        for (String oid : OIDs) {
            pdu.add(new VariableBinding(new OID(oid)));
        }

        // send GET request
        ResponseEvent response;
        try {
            response = snmp.send(pdu, target);
            PDU responsePDU = response.getResponse();
            @SuppressWarnings("rawtypes")
            Vector vbs = responsePDU.getVariableBindings();
            for (int i = 0; i < vbs.size(); i++) {
                VariableBinding vb = (VariableBinding) vbs.get(i);
                result.put(vb.getOid().toString(), vb.getVariable().toString());
            }
        }
        catch (IOException e) {
            throw new ConnectionException("SNMP get request failed! " + e.getMessage());
        }
        catch (NullPointerException e) {
            throw new SnmpTimeoutException("Timeout: Target doesn't respond!");
        }

        return result;
    }

    /**
     * Receives one single OID in string format, creates PDU and sends GET request to defined target. This method is a
     * blocking method. It waits for response.
     *
     * @param OID OID that should be read from target
     * @return String containing read value
     * @throws SnmpTimeoutException
     * @throws ConnectionException
     */
    public String getSingleRequests(String OID) throws SnmpTimeoutException, ConnectionException {

        String result = null;

        // set PDU
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);

        pdu.add(new VariableBinding(new OID(OID)));

        // send GET request
        ResponseEvent response;
        try {
            response = snmp.send(pdu, target);
            PDU responsePDU = response.getResponse();
            @SuppressWarnings("rawtypes")
            Vector vbs = responsePDU.getVariableBindings();
            result = ((VariableBinding) vbs.get(0)).getVariable().toString();
        }
        catch (IOException e) {
            throw new ConnectionException("SNMP get request failed! " + e.getMessage());
        }
        catch (NullPointerException e) {
            throw new SnmpTimeoutException("Timeout: Target doesn't respond!");
        }

        return result;
    }

    @Override
    public String getDeviceAddress() {
        return targetAddress.toString();
    }

    public synchronized void addEventListener(SnmpDiscoveryListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeEventListener(SnmpDiscoveryListener listener) {
        listeners.remove(listener);
    }

    /**
     * This method will call all listeners for given new device
     *
     * @param address     address of device
     * @param version     version of snmp that this device support
     * @param description other extra information which can be useful
     */
    protected synchronized void NotifyForNewDevice(Address address,
                                                   SNMPVersion version,
                                                   String description) {
        SnmpDiscoveryEvent event = new SnmpDiscoveryEvent(this, address, version, description);
        @SuppressWarnings("rawtypes")
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            ((SnmpDiscoveryListener) i.next()).onNewDeviceFound(event);
        }
    }

    /**
     * Calculate and return next broadcast address. (eg. if ip=1.2.3.x, returns 1.2.4.255)
     *
     * @param ip
     * @return String
     */
    public static String getNextBroadcastIPV4Address(String ip) {
        String[] nums = ip.split("\\.");
        int i = (Integer.parseInt(nums[0]) << 24
                 | Integer.parseInt(nums[2]) << 8
                 | Integer.parseInt(nums[1]) << 16
                 | Integer
                .parseInt(nums[3])) + 256;

        return String.format("%d.%d.%d.%d", i >>> 24 & 0xFF, i >> 16 & 0xFF, i >> 8 & 0xFF, 255);
    }

    /**
     * Helper function in order to parse response vector to map structure
     *
     * @param responseVector
     * @return HashMap<String, String>
     */
    public static HashMap<String, String> parseResponseVectorToHashMap(Vector<VariableBinding> responseVector) {

        HashMap<String, String> map = new HashMap<String, String>();
        for (VariableBinding elem : responseVector) {
            map.put(elem.getOid().toString(), elem.getVariable().toString());
        }
        return map;
    }

    protected static String scannerMakeDescriptionString(HashMap<String, String> scannerResult) {
        String desc = "";

        for (String key : ScanOIDs.keySet()) {
            desc += "["
                    + key
                    + "("
                    + ScanOIDs.get(key)
                    + ")="
                    + scannerResult.get(ScanOIDs.get(key))
                    + "] ";
        }

        return desc;
    }

    /**
     * Returns respective SNMPVersion enum value based on given SnmpConstant version value
     *
     * @param version
     * @return SNMPVersion or null if given value is not valid
     */
    protected static SNMPVersion getSnmpVersionFromSnmpConstantsValue(int version) {
        switch (version) {
        case 0:
            return SNMPVersion.V1;
        case 1:
            return SNMPVersion.V2c;
        case 3:
            return SNMPVersion.V3;
        }
        return null;
    }
}