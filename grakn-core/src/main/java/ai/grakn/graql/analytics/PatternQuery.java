/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.graql.analytics;

import ai.grakn.GraknGraph;
import ai.grakn.concept.TypeName;
import ai.grakn.graql.ComputeQuery;

import java.util.Collection;
import java.util.Map;

/**
 * @author Jason Liu
 */
public interface PatternQuery extends ComputeQuery<Map<String, Long>> {

    /**
     * @param subTypeNames an array of types to include in the subgraph
     * @return a PatternQuery with the subTypeNames set
     */
    @Override
    PatternQuery in(String... subTypeNames);

    /**
     * @param subTypeNames a collection of types to include in the subgraph
     * @return a PatternQuery with the subTypeNames set
     */
    @Override
    PatternQuery in(Collection<TypeName> subTypeNames);

    /**
     * @param ofTypeNames an array of types in the subgraph to compute patterns of. By default the patterns of all the
     *                    types in the graph will be computed
     * @return a PatternQuery with the subTypeNames set
     */
    PatternQuery of(String... ofTypeNames);

    /**
     * @param ofTypeNames a collection of types in the subgraph to compute patterns of. By default the patterns of all the
     *                    types in the graph will be computed
     * @return a PatternQuery with the subTypeNames set
     */
    PatternQuery of(Collection<TypeName> ofTypeNames);

    /**
     * @param graph the graph to execute the query on
     * @return a PatternQuery with the graph set
     */
    @Override
    PatternQuery withGraph(GraknGraph graph);
}
