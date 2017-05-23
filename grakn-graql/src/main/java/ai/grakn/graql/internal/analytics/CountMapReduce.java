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

import ai.grakn.concept.TypeId;
import org.apache.tinkerpop.gremlin.process.computer.KeyValue;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The MapReduce program for counting the number of instances in a graph
 * <p>
 *
 * @author Jason Liu
 * @author Sheldon Hall
 */

public class CountMapReduce extends GraknMapReduce<Long> {

    // Needed internally for OLAP tasks
    public CountMapReduce() {
    }

    public CountMapReduce(Set<TypeId> typeIds) {
        super(typeIds);
    }

    @Override
    public void safeMap(final Vertex vertex, final MapEmitter<Serializable, Long> emitter) {
        // use the ghost node detector here again
        if (!selectedTypes.isEmpty()) {
            TypeId vertexTypeId = Utility.getVertexTypeId(vertex);
            if (selectedTypes.contains(vertexTypeId)) {
                emitter.emit(vertexTypeId.getValue(), 1L);
                return;
            }
        } else if (baseTypes.contains(vertex.label())) {
            emitter.emit(NullObject.instance(), 1L);
            return;
        }

        // TODO: this is a bug with hasNext implementation - must send a message
        emitter.emit(NullObject.instance(), 0L);
    }

    @Override
    Long reduceValues(Iterator<Long> values) {
        return IteratorUtils.reduce(values, 0L, (a, b) -> a + b);
    }

    @Override
    public Map<Serializable, Long> generateFinalResult(Iterator<KeyValue<Serializable, Long>> keyValues) {
        Map<Serializable, Long> finalResult = super.generateFinalResult(keyValues);
        finalResult.remove(NullObject.instance());
        return finalResult;
    }
}
