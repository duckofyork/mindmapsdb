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

package ai.grakn.graql.internal.analytics;

import ai.grakn.concept.TypeName;
import ai.grakn.util.Schema;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.Memory;
import org.apache.tinkerpop.gremlin.process.computer.MessageScope;
import org.apache.tinkerpop.gremlin.process.computer.Messenger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * The vertex program for computing frequent patterns of relations.
 * <p>
 *
 * @author Jason Liu
 */

public class PatternVertexProgram extends GraknVertexProgram<String> {

    public static final String FREQUENT_PATTERN = "patternVertexProgram.frequentPattern";

    private static final String OF_TYPE_NAMES = "patternVertexProgram.ofTypeNames";
    private static final Set<String> ELEMENT_COMPUTE_KEYS = Collections.singleton(FREQUENT_PATTERN);

    private Set<TypeName> ofTypeNames = new HashSet<>();

    public PatternVertexProgram() {
    }

    public PatternVertexProgram(Set<TypeName> types, Set<TypeName> ofTypeNames) {
        selectedTypes = types;
        this.ofTypeNames = ofTypeNames;
    }

    @Override
    public void storeState(final Configuration configuration) {
        super.storeState(configuration);
        ofTypeNames.forEach(type -> configuration.addProperty(OF_TYPE_NAMES + "." + type, type));
    }

    @Override
    public void loadState(final Graph graph, final Configuration configuration) {
        super.loadState(graph, configuration);
        configuration.subset(OF_TYPE_NAMES).getKeys().forEachRemaining(key ->
                ofTypeNames.add(TypeName.of(configuration.getProperty(OF_TYPE_NAMES + "." + key).toString())));
    }

    @Override
    public Set<String> getElementComputeKeys() {
        return ELEMENT_COMPUTE_KEYS;
    }

    @Override
    public Set<MessageScope> getMessageScopes(final Memory memory) {
        switch (memory.getIteration()) {
            case 0:
                return Collections.singleton(messageScopeInRolePlayer);
            case 1:
                return Collections.singleton(messageScopeInCasting);
            case 2:
                return Collections.singleton(messageScopeOutCasting);
            case 3:
                return Collections.singleton(messageScopeOutRolePlayer);
            default:
                return Collections.emptySet();
        }
    }

    @Override
    void safeExecute(Vertex vertex, Messenger<String> messenger, Memory memory) {
        TypeName vertexType;
        switch (memory.getIteration()) {
            case 0:
                vertexType = Utility.getVertexType(vertex);
                if (selectedTypes.contains(vertexType) && vertex.label().equals(Schema.BaseType.ENTITY.name())) {
                    messenger.sendMessage(messageScopeInRolePlayer, vertex.id().toString());
                }
                break;
            case 1:
                if (vertex.label().equals(Schema.BaseType.CASTING.name()) && messenger.receiveMessages().hasNext()) {
                    messenger.sendMessage(messageScopeInCasting, messenger.receiveMessages().next());
                }
                break;
            case 2:
                vertexType = Utility.getVertexType(vertex);
                if (selectedTypes.contains(vertexType) && vertex.label().equals(Schema.BaseType.RELATION.name()) &&
                        messenger.receiveMessages().hasNext()) {
                    messenger.receiveMessages().forEachRemaining(message ->
                            messenger.sendMessage(messageScopeOutCasting, vertexType + ":" + message));
                }
                break;
            case 3:
                if (vertex.label().equals(Schema.BaseType.CASTING.name()) && messenger.receiveMessages().hasNext()) {
                    messenger.receiveMessages().forEachRemaining(
                            message -> messenger.sendMessage(messageScopeOutRolePlayer, message));
                }
                break;
            case 4:
                vertexType = Utility.getVertexType(vertex);
                if (ofTypeNames.contains(vertexType) && messenger.receiveMessages().hasNext()) {
                    Set<String> allMessages = new HashSet<>();
                    messenger.receiveMessages().forEachRemaining(allMessages::add);
                    allMessages.remove(vertex.id().toString());
                    if (!allMessages.isEmpty()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        allMessages.stream()
                                .filter(s -> !s.startsWith("has-resource-"))
                                .sorted(Comparator.naturalOrder())
                                .map(s -> s + "\t")
                                .forEach(stringBuilder::append);
                        String patterns = stringBuilder.toString();
                        if (!patterns.isEmpty()) {
                            vertex.property(FREQUENT_PATTERN, patterns);
                        }
                    }
                }
                break;
//            case 0:
//                vertexType = Utility.getVertexType(vertex);
//                if (selectedTypes.contains(vertexType) && vertex.label().equals(Schema.BaseType.RELATION.name())) {
//                    messenger.sendMessage(messageScopeOutCasting, vertexType.getValue());
//
//                }
//                break;
//            case 1:
//                if (vertex.label().equals(Schema.BaseType.CASTING.name()) && messenger.receiveMessages().hasNext()) {
//                    messenger.sendMessage(messageScopeOutRolePlayer, messenger.receiveMessages().next());
//                }
//                break;
//            case 2:
//                vertexType = Utility.getVertexType(vertex);
//                if (!vertex.label().equals(Schema.BaseType.RESOURCE_TYPE.name()) &&
//                        ofTypeNames.contains(vertexType) && messenger.receiveMessages().hasNext()) {
//                    Set<String> allMessages = new HashSet<>();
//                    messenger.receiveMessages().forEachRemaining(allMessages::add);
//                    if (!allMessages.isEmpty()) {
//                        StringBuilder stringBuilder = new StringBuilder();
//                        allMessages.stream()
//                                .filter(s -> !s.startsWith("has-resource-"))
//                                .sorted(Comparator.naturalOrder())
//                                .map(s -> s + "\t")
//                                .forEach(stringBuilder::append);
//                        String patterns = stringBuilder.toString();
//                        if (!patterns.isEmpty()) {
//                            vertex.property(FREQUENT_PATTERN, patterns);
//                        }
//                    }
//                }
//                break;
            default:
                throw new RuntimeException("unreachable");
        }
    }

    @Override
    public boolean terminate(final Memory memory) {
        LOGGER.debug("Finished Pattern Iteration " + memory.getIteration());
        return memory.getIteration() == 4;
    }
}
