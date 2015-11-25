package service;

import domain.BaseNode;
import domain.BaseRelationship;
import exception.DatabaseOperationException;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.cypherdsl.CypherQuery;
import org.neo4j.cypherdsl.grammar.Execute;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.neo4j.cypherdsl.CypherQuery.*;
import static util.CustomCypher.toPropertyValues;

@Service
public class Neo4jService {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4jService.class);

    @Autowired
    private RestGraphDatabase database;

    @Autowired
    private RestCypherQueryEngine engine;

    public Set<String> labels() {
        return this.database.getAllLabelNames().stream().collect(Collectors.toSet());
    }

    public Set<String> relationships() {
        return StreamSupport.stream(this.database.getRelationshipTypes().spliterator(), false).map(it -> it.name()).collect(Collectors.toSet());
    }

    public BaseNode save(BaseNode node) throws DatabaseOperationException {
        try {
            Assert.notNull(node, "BaseNode is null");

            Node _node = (node.getId() != null ? this.database.getNodeById(node.getId()) : null);
            if (_node == null) {
                _node = this.database.createNode(node.getLabels().stream().map(it -> DynamicLabel.label(it)).toArray(Label[]::new));
                Assert.state(_node != null, "Unable to create node");
            }

            for (String key: _node.getPropertyKeys()) {
                _node.removeProperty(key);
            }

            for (String key: node.keySet()) {
                _node.setProperty(key, node.get(key));
            }

            node.setNode(_node);
            node.setId(_node.getId());

            return node;
        } catch (Exception exception) {
            LOG.error(exception.getMessage(), exception);
            throw new DatabaseOperationException(exception.getMessage(), exception);
        }
    }

    public void delete(BaseNode node) throws DatabaseOperationException {
        try {
            Assert.notNull(node, "BaseNode is null");
            Assert.notNull(node.getId(), "Node.id is null");

            Node _node = this.database.getNodeById(node.getId());
            if (_node.hasRelationship()) {
                for (Relationship relationship: _node.getRelationships()) {
                    relationship.delete();
                }
                _node.delete();
            }
        } catch (Exception exception) {
            LOG.error(exception.getMessage(), exception);
            throw new DatabaseOperationException(exception.getMessage(), exception);
        }
    }

    public BaseRelationship relate(BaseNode origin, BaseRelationship relationship, BaseNode target) throws DatabaseOperationException {
        try {
            Assert.notNull(origin, "Origin BaseNode is null");
            Assert.notNull(target, "Target BaseNode is null");
            Assert.notNull(relationship, "Relationship is null");
            Assert.notNull(relationship.getType(), "RelationshipType is null");

            Map<String, Object> param = new HashMap<>();

            Execute query = CypherQuery
                    .start(
                            nodesById("origin", origin.getNode().getId()),
                            nodesById("target", target.getNode().getId()))
                    .createUnique(
                            node("origin").out(relationship.getType()).values(toPropertyValues(param, relationship)).as("relationship").node("target"))
                    .returns(identifier("relationship"))
                    ;

            return StreamSupport.stream(this.engine.query(query.toString(), param).spliterator(), false).findFirst().map(BaseRelationship.converter("relationship")).orElse(null);
        } catch (Exception exception) {
            LOG.error(exception.getMessage(), exception);
            throw new DatabaseOperationException(exception.getMessage(), exception);
        }
    }

    public void unrelate(BaseRelationship relationship) throws DatabaseOperationException {
        try {
            Assert.notNull(relationship, "Relationship is null");
            Assert.notNull(relationship.getId(), "Relationship.id is null");

            Map<String, Object> param = new HashMap<>();

            Execute query = CypherQuery
                    .start(relationshipsById("relationship", relationship.getId()))
                    .delete(identifier("relationship"))
                    ;

            this.engine.query(query.toString(), param);
        } catch (Exception exception) {
            LOG.error(exception.getMessage(), exception);
            throw new DatabaseOperationException(exception.getMessage(), exception);
        }
    }

    public BaseNode get(Long id) throws DatabaseOperationException {
        try {
            Assert.notNull(id, "ID is null");
            return new BaseNode(this.database.getNodeById(id));
        } catch (Exception exception) {
            LOG.error(exception.getMessage(), exception);
            throw new DatabaseOperationException(exception.getMessage(), exception);
        }
    }

    public Set<BaseNode> get(String label, Pair<String, Object>... pairs) throws DatabaseOperationException {
        try {
            Assert.notNull(label, "Label is null");
            Assert.notEmpty(pairs, "Properties are null");

            Map<String, Object> param = new HashMap<>();

            Execute query = CypherQuery
                    .match(node("node").label(label).values(toPropertyValues(param, pairs)))
                    .returns(identifier("node"));

            return StreamSupport.stream(this.engine.query(query.toString(), param).spliterator(), false).map(BaseNode.converter("node")).collect(Collectors.toSet());
        } catch (Exception exception) {
            LOG.error(exception.getMessage(), exception);
            throw new DatabaseOperationException(exception.getMessage(), exception);
        }
    }
}
