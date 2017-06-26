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

package ai.grakn.graql.internal.gremlin.fragment;

import ai.grakn.GraknGraph;
import ai.grakn.graql.Var;
import ai.grakn.graql.internal.gremlin.spanningtree.graph.DirectedEdge;
import ai.grakn.graql.internal.gremlin.spanningtree.graph.Node;
import ai.grakn.graql.internal.gremlin.spanningtree.util.Weighted;
import ai.grakn.graql.admin.VarProperty;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Map;
import java.util.Set;

import static ai.grakn.util.Schema.EdgeLabel.RELATES;

class InRelatesFragment extends AbstractFragment {

    InRelatesFragment(VarProperty varProperty, Var start, Var end) {
        super(varProperty, start, end);
    }

    @Override
    public void applyTraversal(GraphTraversal<Vertex, Vertex> traversal, GraknGraph graph) {
        traversal.in(RELATES.getLabel());
    }

    @Override
    public String getName() {
        return "<-[relates]-";
    }

    @Override
    public double fragmentCost(double previousCost) {
        return COST_SAME_AS_PREVIOUS;
    }

    @Override
    public Set<Weighted<DirectedEdge<Node>>> getDirectedEdges(Map<Node, Map<Node, Fragment>> edgeToFragment) {
        return getDirectedEdgesIn("(relates)", edgeToFragment);
    }
}
