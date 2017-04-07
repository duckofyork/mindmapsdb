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

package ai.grakn.test.graql.analytics;

import ai.grakn.Grakn;
import ai.grakn.GraknGraph;
import ai.grakn.GraknSession;
import ai.grakn.GraknTxType;
import ai.grakn.concept.ConceptId;
import ai.grakn.concept.Entity;
import ai.grakn.concept.EntityType;
import ai.grakn.concept.RelationType;
import ai.grakn.concept.ResourceType;
import ai.grakn.concept.RoleType;
import ai.grakn.concept.TypeName;
import ai.grakn.exception.GraknValidationException;
import ai.grakn.graph.internal.computer.GraknSparkComputer;
import ai.grakn.test.EngineContext;
import ai.grakn.util.Schema;
import org.apache.tinkerpop.gremlin.spark.structure.Spark;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ai.grakn.test.GraknTestEnv.usingOrientDB;
import static ai.grakn.test.GraknTestEnv.usingTinker;
import static org.junit.Assume.assumeFalse;

public class PatternTest {

    private static final String thing = "thing";
    private static final String anotherThing = "anotherThing";

    private static final String resourceType1 = "resourceType1";
    private static final String resourceType2 = "resourceType2";
    private static final String resourceType3 = "resourceType3";
    private static final String resourceType4 = "resourceType4";
    private static final String resourceType5 = "resourceType5";
    private static final String resourceType6 = "resourceType6";
    private static final String resourceType7 = "resourceType7";

    private static final double delta = 0.000001;

    private ConceptId entityId1;
    private ConceptId entityId2;
    private ConceptId entityId3;
    private ConceptId entityId4;

    private GraknSession graknSession;

//    @ClassRule
//    public static final EngineContext context = EngineContext.startInMemoryServer();
//
//    private GraknSession factory;
//
//    @Before
//    public void setUp() {
//        // TODO: Fix tests in orientdb
//        assumeFalse(usingOrientDB());
//
//        GraknGraph graph = Grakn.session("localhost", "ihsmarkit").open(GraknTxType.READ);
//
//    }

    @Test
    public void testMinAndMax() throws Exception {
        // TODO: Fix in TinkerGraphComputer
//        assumeFalse(usingTinker());

        GraknSparkComputer.close();

        Map<String, Long> result;

//        // resource-type has no instance
//        addOntologyAndEntities();
//        // add resources, but resources are not connected to any entities
//        addResourcesInstances();
//        // connect entity and resources
//        addResourceRelations();

        try (GraknSession session = Grakn.session(Grakn.DEFAULT_URI, "ihsmarkit")) {
            // open a graph (database transaction)
            try (GraknGraph graph = session.open(GraknTxType.READ)) {
                result = graph.graql().compute().patterns().of("energy-asset").execute();
            }
        }
        System.out.println("result = " + result);
    }

    private void addOntologyAndEntities() throws GraknValidationException {
        try (GraknGraph graph = graknSession.open(GraknTxType.WRITE)) {
            EntityType entityType1 = graph.putEntityType(thing);
            EntityType entityType2 = graph.putEntityType(anotherThing);

            Entity entity1 = entityType1.addEntity();
            Entity entity2 = entityType1.addEntity();
            Entity entity3 = entityType1.addEntity();
            Entity entity4 = entityType2.addEntity();
            entityId1 = entity1.getId();
            entityId2 = entity2.getId();
            entityId3 = entity3.getId();
            entityId4 = entity4.getId();

            RoleType relation1 = graph.putRoleType("relation1");
            RoleType relation2 = graph.putRoleType("relation2");
            entityType1.playsRole(relation1).playsRole(relation2);
            entityType2.playsRole(relation1).playsRole(relation2);
            RelationType related = graph.putRelationType("related").hasRole(relation1).hasRole(relation2);

            related.addRelation()
                    .addRolePlayer(relation1, entity1)
                    .addRolePlayer(relation2, entity2);
            related.addRelation()
                    .addRolePlayer(relation1, entity2)
                    .addRolePlayer(relation2, entity3);
            related.addRelation()
                    .addRolePlayer(relation1, entity2)
                    .addRolePlayer(relation2, entity4);

            List<ResourceType> resourceTypeList = new ArrayList<>();
            resourceTypeList.add(graph.putResourceType(resourceType1, ResourceType.DataType.DOUBLE));
            resourceTypeList.add(graph.putResourceType(resourceType2, ResourceType.DataType.LONG));
            resourceTypeList.add(graph.putResourceType(resourceType3, ResourceType.DataType.LONG));
            resourceTypeList.add(graph.putResourceType(resourceType4, ResourceType.DataType.STRING));
            resourceTypeList.add(graph.putResourceType(resourceType5, ResourceType.DataType.LONG));
            resourceTypeList.add(graph.putResourceType(resourceType6, ResourceType.DataType.DOUBLE));
            resourceTypeList.add(graph.putResourceType(resourceType7, ResourceType.DataType.DOUBLE));

            RoleType resourceOwner1 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType1)));
            RoleType resourceOwner2 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType2)));
            RoleType resourceOwner3 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType3)));
            RoleType resourceOwner4 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType4)));
            RoleType resourceOwner5 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType5)));
            RoleType resourceOwner6 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType6)));
            RoleType resourceOwner7 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType7)));

            RoleType resourceValue1 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType1)));
            RoleType resourceValue2 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType2)));
            RoleType resourceValue3 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType3)));
            RoleType resourceValue4 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType4)));
            RoleType resourceValue5 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType5)));
            RoleType resourceValue6 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType6)));
            RoleType resourceValue7 = graph.putRoleType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType7)));

            graph.putRelationType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType1)))
                    .hasRole(resourceOwner1).hasRole(resourceValue1);
            graph.putRelationType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType2)))
                    .hasRole(resourceOwner2).hasRole(resourceValue2);
            graph.putRelationType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType3)))
                    .hasRole(resourceOwner3).hasRole(resourceValue3);
            graph.putRelationType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType4)))
                    .hasRole(resourceOwner4).hasRole(resourceValue4);
            graph.putRelationType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType5)))
                    .hasRole(resourceOwner5).hasRole(resourceValue5);
            graph.putRelationType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType6)))
                    .hasRole(resourceOwner6).hasRole(resourceValue6);
            graph.putRelationType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType7)))
                    .hasRole(resourceOwner7).hasRole(resourceValue7);

            entityType1.playsRole(resourceOwner1)
                    .playsRole(resourceOwner2)
                    .playsRole(resourceOwner3)
                    .playsRole(resourceOwner4)
                    .playsRole(resourceOwner5)
                    .playsRole(resourceOwner6)
                    .playsRole(resourceOwner7);
            entityType2.playsRole(resourceOwner1)
                    .playsRole(resourceOwner2)
                    .playsRole(resourceOwner3)
                    .playsRole(resourceOwner4)
                    .playsRole(resourceOwner5)
                    .playsRole(resourceOwner6)
                    .playsRole(resourceOwner7);

            resourceTypeList.forEach(resourceType -> resourceType
                    .playsRole(resourceValue1)
                    .playsRole(resourceValue2)
                    .playsRole(resourceValue3)
                    .playsRole(resourceValue4)
                    .playsRole(resourceValue5)
                    .playsRole(resourceValue6)
                    .playsRole(resourceValue7));

            graph.commit();
        }
    }

    private void addResourcesInstances() throws GraknValidationException {
        try (GraknGraph graph = graknSession.open(GraknTxType.WRITE)) {
            graph.<Double>getResourceType(resourceType1).putResource(1.2);
            graph.<Double>getResourceType(resourceType1).putResource(1.5);
            graph.<Double>getResourceType(resourceType1).putResource(1.8);

            graph.<Long>getResourceType(resourceType2).putResource(4L);
            graph.<Long>getResourceType(resourceType2).putResource(-1L);
            graph.<Long>getResourceType(resourceType2).putResource(0L);

            graph.<Long>getResourceType(resourceType5).putResource(6L);
            graph.<Long>getResourceType(resourceType5).putResource(7L);
            graph.<Long>getResourceType(resourceType5).putResource(8L);

            graph.<Double>getResourceType(resourceType6).putResource(7.2);
            graph.<Double>getResourceType(resourceType6).putResource(7.5);
            graph.<Double>getResourceType(resourceType6).putResource(7.8);

            graph.<String>getResourceType(resourceType4).putResource("a");
            graph.<String>getResourceType(resourceType4).putResource("b");
            graph.<String>getResourceType(resourceType4).putResource("c");

            graph.commit();
        }
    }

    private void addResourceRelations() throws GraknValidationException {
        try (GraknGraph graph = graknSession.open(GraknTxType.WRITE)) {
            Entity entity1 = graph.getConcept(entityId1);
            Entity entity2 = graph.getConcept(entityId2);
            Entity entity3 = graph.getConcept(entityId3);
            Entity entity4 = graph.getConcept(entityId4);

            RoleType resourceOwner1 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType1)));
            RoleType resourceOwner2 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType2)));
            RoleType resourceOwner3 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType3)));
            RoleType resourceOwner4 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType4)));
            RoleType resourceOwner5 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType5)));
            RoleType resourceOwner6 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_OWNER.getName(TypeName.of(resourceType6)));

            RoleType resourceValue1 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType1)));
            RoleType resourceValue2 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType2)));
            RoleType resourceValue3 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType3)));
            RoleType resourceValue4 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType4)));
            RoleType resourceValue5 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType5)));
            RoleType resourceValue6 = graph.getType(Schema.ImplicitType.HAS_RESOURCE_VALUE.getName(TypeName.of(resourceType6)));

            RelationType relationType1 = graph.getType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType1)));
            relationType1.addRelation()
                    .addRolePlayer(resourceOwner1, entity1)
                    .addRolePlayer(resourceValue1, graph.<Double>getResourceType(resourceType1).putResource(1.2));
            relationType1.addRelation()
                    .addRolePlayer(resourceOwner1, entity1)
                    .addRolePlayer(resourceValue1, graph.<Double>getResourceType(resourceType1).putResource(1.5));
            relationType1.addRelation()
                    .addRolePlayer(resourceOwner1, entity3)
                    .addRolePlayer(resourceValue1, graph.<Double>getResourceType(resourceType1).putResource(1.8));

            RelationType relationType2 = graph.getType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType2)));
            relationType2.addRelation()
                    .addRolePlayer(resourceOwner2, entity1)
                    .addRolePlayer(resourceValue2, graph.<Long>getResourceType(resourceType2).putResource(4L));
            relationType2.addRelation()
                    .addRolePlayer(resourceOwner2, entity1)
                    .addRolePlayer(resourceValue2, graph.<Long>getResourceType(resourceType2).putResource(-1L));
            relationType2.addRelation()
                    .addRolePlayer(resourceOwner2, entity4)
                    .addRolePlayer(resourceValue2, graph.<Long>getResourceType(resourceType2).putResource(0L));

            graph.<Long>getResourceType(resourceType3).putResource(100L);

            RelationType relationType5 = graph.getType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType5)));
            relationType5.addRelation()
                    .addRolePlayer(resourceOwner5, entity1)
                    .addRolePlayer(resourceValue5, graph.<Long>getResourceType(resourceType5).putResource(-7L));
            relationType5.addRelation()
                    .addRolePlayer(resourceOwner5, entity2)
                    .addRolePlayer(resourceValue5, graph.<Long>getResourceType(resourceType5).putResource(-7L));
            relationType5.addRelation()
                    .addRolePlayer(resourceOwner5, entity4)
                    .addRolePlayer(resourceValue5, graph.<Long>getResourceType(resourceType5).putResource(-7L));

            RelationType relationType6 = graph.getType(Schema.ImplicitType.HAS_RESOURCE.getName(TypeName.of(resourceType6)));
            relationType6.addRelation()
                    .addRolePlayer(resourceOwner6, entity1)
                    .addRolePlayer(resourceValue6, graph.<Double>getResourceType(resourceType6).putResource(7.5));
            relationType6.addRelation()
                    .addRolePlayer(resourceOwner6, entity2)
                    .addRolePlayer(resourceValue6, graph.<Double>getResourceType(resourceType6).putResource(7.5));
            relationType6.addRelation()
                    .addRolePlayer(resourceOwner6, entity4)
                    .addRolePlayer(resourceValue6, graph.<Double>getResourceType(resourceType6).putResource(7.5));

            // some resources in, but not connect them to any instances
            graph.<Double>getResourceType(resourceType1).putResource(2.8);
            graph.<Long>getResourceType(resourceType2).putResource(-5L);
            graph.<Long>getResourceType(resourceType5).putResource(10L);
            graph.<Double>getResourceType(resourceType6).putResource(0.8);

            graph.commit();
        }
    }
}
