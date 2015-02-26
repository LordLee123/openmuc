/*
 * Copyright 2011-15 Fraunhofer ISE
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

package org.openmuc.framework.core.datamanager;

import org.openmuc.framework.config.*;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public final class RootConfigImpl implements RootConfig {

    private String dataLogSource = null;

    final HashMap<String, DriverConfigImpl> driverConfigsById = new LinkedHashMap<String, DriverConfigImpl>();
    final HashMap<String, DeviceConfigImpl> deviceConfigsById = new HashMap<String, DeviceConfigImpl>();
    final HashMap<String, ChannelConfigImpl> channelConfigsById = new HashMap<String, ChannelConfigImpl>();

    // TODO really needed?:
    List<LogChannel> logChannels;

    @Override
    public String getDataLogSource() {
        return dataLogSource;
    }

    @Override
    public void setDataLogSource(String source) {
        dataLogSource = source;
    }

    static RootConfigImpl createFromFile(File configFile) throws ParseException, FileNotFoundException {
        if (configFile == null) {
            throw new NullPointerException("configFileName is null or the empty string.");
        }

        if (!configFile.exists()) {
            throw new FileNotFoundException();
        }

        DocumentBuilderFactory docBFac = DocumentBuilderFactory.newInstance();
        docBFac.setIgnoringComments(true);

        Document doc;
        try {
            doc = docBFac.newDocumentBuilder().parse(configFile);
        } catch (Exception e) {
            throw new ParseException(e);
        }

        Node rootNode = doc.getDocumentElement();

        if (!rootNode.getNodeName().equals("configuration")) {
            throw new ParseException("root node in configuration is not of type \"configuration\"");
        }

        return getRootConfigFromDomNode(rootNode);

    }

    static RootConfigImpl getRootConfigFromDomNode(Node rootConfigNode) throws ParseException {

        RootConfigImpl rootConfig = new RootConfigImpl();

        NodeList rootConfigChildren = rootConfigNode.getChildNodes();

        for (int i = 0; i < rootConfigChildren.getLength(); i++) {
            Node childNode = rootConfigChildren.item(i);
            String childName = childNode.getNodeName();
            if (childName.equals("#text")) {
                continue;
            } else if (childName.equals("driver")) {
                DriverConfigImpl.addDriverFromDomNode(childNode, rootConfig);
            } else if (childName.equals("dataLogSource")) {
                rootConfig.dataLogSource = childNode.getTextContent();
            } else {
                throw new ParseException("found unknown tag:" + childName);
            }
        }

        return rootConfig;
    }

    void writeToFile(File configFile) throws TransformerFactoryConfigurationError, IOException, ParserConfigurationException,
            TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StreamResult result = new StreamResult(new FileWriter(configFile));

        DocumentBuilder docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuild.newDocument();

        doc.appendChild(getDomElement(doc));
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

    }

    Element getDomElement(Document document) {
        Element rootConfigElement = document.createElement("configuration");

        if (dataLogSource != null) {
            Node loggerChild = document.createElement("dataLogSource");
            loggerChild.setTextContent(dataLogSource);
            rootConfigElement.appendChild(loggerChild);
        }

        for (DriverConfig driverConfig : driverConfigsById.values()) {
            rootConfigElement.appendChild(((DriverConfigImpl) driverConfig).getDomElement(document));
        }

        return rootConfigElement;
    }

    @Override
    public DriverConfig getOrAddDriver(String id) {
        try {
            return addDriver(id);
        } catch (IdCollisionException e) {
            return driverConfigsById.get(id);
        }
    }

    @Override
    public DriverConfig addDriver(String id) throws IdCollisionException {
        if (id == null) {
            throw new IllegalArgumentException("The driver ID may not be null");
        }
        ChannelConfigImpl.checkIdSyntax(id);

        if (driverConfigsById.containsKey(id)) {
            throw new IdCollisionException("Collision with the driver ID:" + id);
        }
        DriverConfigImpl driverConfig = new DriverConfigImpl(id, this);
        driverConfigsById.put(id, driverConfig);
        return driverConfig;
    }

    @Override
    public DriverConfig getDriver(String id) {
        return driverConfigsById.get(id);
    }

    @Override
    public DeviceConfig getDevice(String id) {
        return deviceConfigsById.get(id);
    }

    @Override
    public ChannelConfig getChannel(String id) {
        return channelConfigsById.get(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DriverConfig> getDrivers() {
        return (Collection<DriverConfig>) (Collection<?>) Collections.unmodifiableCollection(driverConfigsById.values());
    }

    @Override
    protected RootConfigImpl clone() {
        RootConfigImpl configClone = new RootConfigImpl();
        configClone.dataLogSource = dataLogSource;
        for (DriverConfigImpl driverConfig : driverConfigsById.values()) {
            configClone.addDriver(driverConfig.clone(configClone));
        }
        return configClone;
    }

    RootConfigImpl cloneWithDefaults() {
        RootConfigImpl configClone = new RootConfigImpl();
        if (dataLogSource != null) {
            configClone.dataLogSource = dataLogSource;
        } else {
            configClone.dataLogSource = "";
        }
        for (DriverConfigImpl driverConfig : driverConfigsById.values()) {
            configClone.addDriver(driverConfig.cloneWithDefaults(configClone));
        }
        return configClone;
    }

    private void addDriver(DriverConfigImpl driverConfig) {
        driverConfigsById.put(driverConfig.getId(), driverConfig);
        for (DeviceConfigImpl deviceConfig : driverConfig.deviceConfigsById.values()) {
            deviceConfigsById.put(deviceConfig.getId(), deviceConfig);
            for (ChannelConfigImpl channelConfig : deviceConfig.channelConfigsById.values()) {
                channelConfigsById.put(channelConfig.getId(), channelConfig);
            }
        }
    }

}
