package io.mindmaps;/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.mindmaps.api.*;
import io.mindmaps.util.ConfigProperties;

import java.net.URISyntaxException;

import static spark.Spark.port;
import static spark.Spark.staticFileLocation;
import static spark.Spark.staticFiles;

public class MindmapsEngineServer {

    public static void main(String[] args) {

        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.INFO);

        ConfigProperties prop = ConfigProperties.getInstance();

        // Listening port
        port(prop.getPropertyAsInt(ConfigProperties.SERVER_PORT_NUMBER));
        staticFiles.externalLocation(ConfigProperties.getProjectPath()+prop.getProperty(ConfigProperties.STATIC_FILES_PATH));

        // ----- APIs --------- //

        new RemoteShellController();
        new VisualiserController();
        new GraphFactoryController();
        new ImportController();
        new CommitLogController();
        new TransactionController();

    }
}
