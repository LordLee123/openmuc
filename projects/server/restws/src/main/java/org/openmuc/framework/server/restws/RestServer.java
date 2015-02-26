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
package org.openmuc.framework.server.restws;

import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.server.restws.servlets.AuthenticationServlet;
import org.openmuc.framework.server.restws.servlets.ChannelResourceServlet;
import org.openmuc.framework.server.restws.servlets.DeviceResourceServlet;
import org.openmuc.framework.server.restws.servlets.DriverResourceServlet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RestServer {

    private final static Logger logger = LoggerFactory.getLogger(RestServer.class);

    private static DataAccessService dataAccessService;
    private static ConfigService configService;
    private static HttpService httpService;

    private final ChannelResourceServlet chRServlet = new ChannelResourceServlet();
    private final DeviceResourceServlet devRServlet = new DeviceResourceServlet();
    private final DriverResourceServlet drvRServlet = new DriverResourceServlet();
    private final AuthenticationServlet addAuthServlet = new AuthenticationServlet();

    protected void activate(ComponentContext context) throws Exception {

        logger.info("Activating REST Server");

        httpService.registerServlet(Const.ALIAS_CHANNELS, chRServlet, null, null);
        httpService.registerServlet(Const.ALIAS_DEVICES, devRServlet, null, null);
        httpService.registerServlet(Const.ALIAS_DRIVERS, drvRServlet, null, null);
        httpService.registerServlet(Const.ALIAS_AUTHENTICATIONS, addAuthServlet, null, null);
    }

    protected void deactivate(ComponentContext context) {

        logger.info("Deactivating REST Server");

        httpService.unregister(Const.ALIAS_CHANNELS);
        httpService.unregister(Const.ALIAS_DEVICES);
        httpService.unregister(Const.ALIAS_DRIVERS);
        httpService.unregister(Const.ALIAS_AUTHENTICATIONS);

        // TODO wait for all servlets to finish their last tasks?
    }

    protected void setConfigService(ConfigService configService) {
        RestServer.configService = configService;
    }

    protected void unsetConfigService(ConfigService configService) {
        RestServer.configService = null;
    }

    protected void setHttpService(HttpService httpService) {
        RestServer.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        RestServer.httpService = null;
    }

    protected void setDataAccessService(DataAccessService dataAccessService) {
        RestServer.dataAccessService = dataAccessService;
    }

    protected void unsetDataAccessService(DataAccessService dataAccessService) {
        RestServer.dataAccessService = null;
    }

    public static DataAccessService getDataAccessService() {
        return dataAccessService;
    }

    public static ConfigService getConfigService() {
        return configService;
    }

}
