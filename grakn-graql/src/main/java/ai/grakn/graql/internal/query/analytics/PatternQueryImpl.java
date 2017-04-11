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

package ai.grakn.graql.internal.query.analytics;

import ai.grakn.GraknGraph;
import ai.grakn.concept.Concept;
import ai.grakn.concept.ConceptId;
import ai.grakn.concept.TypeName;
import ai.grakn.graql.analytics.PatternQuery;
import ai.grakn.graql.internal.analytics.ClusterSizeMapReduce;
import ai.grakn.graql.internal.analytics.PatternVertexProgram;
import ai.grakn.graql.internal.util.StringConverter;
import ai.grakn.util.ErrorMessage;
import com.google.common.collect.Sets;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ai.grakn.graql.Graql.var;
import static java.util.stream.Collectors.joining;

class PatternQueryImpl extends AbstractComputeQuery<Map<String, Long>> implements PatternQuery {

    private boolean ofTypeNamesSet = false;
    private Set<TypeName> ofTypeNames = new HashSet<>();

    PatternQueryImpl(Optional<GraknGraph> graph) {
        this.graph = graph;
    }

    @Override
    public Map<String, Long> execute() {
        LOGGER.info("PatternQueryVertexProgram is called");
        long startTime = System.currentTimeMillis();
        initSubGraph();
        if (!selectedTypesHaveInstance()) return Collections.emptyMap();
        ofTypeNames.forEach(type -> {
            if (!subTypeNames.contains(type)) {
                throw new IllegalStateException(ErrorMessage.ILLEGAL_ARGUMENT_EXCEPTION
                        .getMessage(type));
            }
        });

        ComputerResult result;

        Set<TypeName> withResourceRelationTypes = getHasResourceRelationTypes();
        withResourceRelationTypes.addAll(subTypeNames);

        if (ofTypeNames.isEmpty()) {
            ofTypeNames.addAll(subTypeNames);
        }

        result = getGraphComputer().compute(new PatternVertexProgram(withResourceRelationTypes, ofTypeNames),
                new ClusterSizeMapReduce(ofTypeNames, PatternVertexProgram.FREQUENT_PATTERN));

        LOGGER.info("PatternVertexProgram is done in " + (System.currentTimeMillis() - startTime) + " ms");
        Map<String, Long> patternMap = result.memory().get(ClusterSizeMapReduce.class.getName());
        final Map<String, Long> patternValueMap = new HashMap<>();
        patternMap.forEach((oldKey, oldValue) -> {
            String key = Arrays.stream(oldKey.split("\t")).map(combo -> {
                String[] pair = combo.split(":");
                List<Map<String, Concept>> matchResultMap = graph.get().graql()
                        .match(var().id(ConceptId.of(pair[1])).has("name", var("name")))
                        .execute();
                if (matchResultMap.isEmpty()) {
                    return combo;
                }
                return pair[0] + " : " + matchResultMap.get(0).get("name").asResource().getValue().toString();
            }).distinct().collect(Collectors.joining("\t"));
            patternValueMap.put(key, oldValue);
        });
        return patternValueMap;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public PatternQuery in(String... subTypeNames) {
        return (PatternQuery) super.in(subTypeNames);
    }

    @Override
    public PatternQuery in(Collection<TypeName> subTypeNames) {
        return (PatternQuery) super.in(subTypeNames);
    }

    @Override
    public PatternQuery of(String... ofTypeNames) {
        if (ofTypeNames.length > 0) {
            ofTypeNamesSet = true;
            this.ofTypeNames = Arrays.stream(ofTypeNames).map(TypeName::of).collect(Collectors.toSet());
        }
        return this;
    }

    @Override
    public PatternQuery of(Collection<TypeName> ofTypeNames) {
        if (!ofTypeNames.isEmpty()) {
            ofTypeNamesSet = true;
            this.ofTypeNames = Sets.newHashSet(ofTypeNames);
        }
        return this;
    }

    @Override
    String graqlString() {
        String string = "patterns";
        if (ofTypeNamesSet) {
            string += " of " + ofTypeNames.stream()
                    .map(StringConverter::typeNameToString)
                    .collect(joining(", "));
        }
        string += subtypeString();
        return string;
    }

    @Override
    public PatternQuery withGraph(GraknGraph graph) {
        return (PatternQuery) super.withGraph(graph);
    }
}
