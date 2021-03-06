/*
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

package io.mindmaps.api;

import io.mindmaps.MindmapsTransaction;
import io.mindmaps.constants.ErrorMessage;
import io.mindmaps.constants.RESTUtil;
import io.mindmaps.core.model.Concept;
import io.mindmaps.factory.GraphFactory;
import io.mindmaps.util.ConfigProperties;
import io.mindmaps.visualiser.HALConcept;
import spark.Request;
import spark.Response;

import static spark.Spark.get;

public class VisualiserController {

    private String defaultGraphName;

    public VisualiserController() {

        defaultGraphName = ConfigProperties.getInstance().getProperty(ConfigProperties.DEFAULT_GRAPH_NAME_PROPERTY);

        get(RESTUtil.WebPath.CONCEPT_BY_ID_URI + RESTUtil.Request.ID_PARAMETER, this::getConceptById);

    }

    private String getConceptById(Request req, Response res) {

        String graphNameParam = req.queryParams(RESTUtil.Request.GRAPH_NAME_PARAM);
        String currentGraphName = (graphNameParam == null) ? defaultGraphName : graphNameParam;

        MindmapsTransaction transaction = GraphFactory.getInstance().getGraph(currentGraphName).getTransaction();

        Concept concept = transaction.getConcept(req.params(RESTUtil.Request.ID_PARAMETER));
        if (concept != null)
            return new HALConcept(concept).render();
        else {
            res.status(404);
            return ErrorMessage.CONCEPT_ID_NOT_FOUND.getMessage(req.params(RESTUtil.Request.ID_PARAMETER));
        }
    }

}
