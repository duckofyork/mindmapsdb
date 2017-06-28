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
import ai.grakn.graql.admin.ValuePredicateAdmin;
import ai.grakn.graql.admin.VarPatternAdmin;
import ai.grakn.graql.admin.VarProperty;
import ai.grakn.graql.internal.gremlin.spanningtree.graph.DirectedEdge;
import ai.grakn.graql.internal.gremlin.spanningtree.graph.Node;
import ai.grakn.graql.internal.gremlin.spanningtree.util.Weighted;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ai.grakn.graql.internal.gremlin.spanningtree.util.Weighted.weighted;
import static ai.grakn.util.CommonUtil.optionalToStream;
import static java.util.stream.Collectors.toSet;

class ValueFragment extends AbstractFragment {

    private final ValuePredicateAdmin predicate;

    ValueFragment(VarProperty varProperty, Var start, ValuePredicateAdmin predicate) {
        super(varProperty, start);
        this.predicate = predicate;
    }

    @Override
    public void applyTraversal(GraphTraversal<Vertex, Vertex> traversal, GraknGraph graph) {
        predicate.applyPredicate(traversal);
    }

    @Override
    public String getName() {
        return "[value:" + predicate + "]";
    }

    @Override
    public double fragmentCost(double previousCost) {
        if (predicate.isSpecific()) {
            return COST_RESOURCES_PER_VALUE;
        } else {
            // Assume approximately half of values will satisfy a filter
            return COST_UNSPECIFIC_PREDICATE;
        }
    }

    @Override
    public boolean hasFixedFragmentCost() {
        return predicate.isSpecific() && getDependencies().isEmpty();
    }

    @Override
    public Set<Var> getDependencies() {
        return optionalToStream(predicate.getInnerVar()).map(VarPatternAdmin::getVarName).collect(toSet());
    }

    @Override
    public Set<Weighted<DirectedEdge<Node>>> getDirectedEdges(Map<String, Node> nodes,
                                                              Map<Node, Map<Node, Fragment>> edges) {
        if (getDependencies().isEmpty()) {
            return super.getDirectedEdges(nodes, edges);
        }
        Set<Weighted<DirectedEdge<Node>>> weightedEdges = new HashSet<>();
        Node end = Node.addIfAbsent(getStart(), nodes);
        getDependencies().forEach(var -> {
            Node start = Node.addIfAbsent(var, nodes);
            Node middle = Node.addIfAbsent(start.getName() + "(value:" + predicate + ")" + end.getName(), nodes);
            addEdgeToFragmentMapping(middle, start, edges);
            weightedEdges.add(weighted(DirectedEdge.from(start).to(middle), -fragmentCost(0)));
            weightedEdges.add(weighted(DirectedEdge.from(middle).to(end), 0));
        });
        return weightedEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ValueFragment that = (ValueFragment) o;

        return predicate != null ? predicate.equals(that.predicate) : that.predicate == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (predicate != null ? predicate.hashCode() : 0);
        return result;
    }
}
