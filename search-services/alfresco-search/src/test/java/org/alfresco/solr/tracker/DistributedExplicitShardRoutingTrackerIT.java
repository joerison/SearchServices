/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.solr.tracker;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.core.SolrCore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.alfresco.repo.search.adaptor.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.tracker.DocRouterFactory.SHARD_KEY_KEY;

/**
 * Test Routes based on an explicit shard
 *
 * @author Gethin James
 */
@SolrTestCaseJ4.SuppressSSL
@SolrTestCaseJ4.SuppressObjectReleaseTracker (bugUrl = "RAMDirectory")
public class DistributedExplicitShardRoutingTrackerIT extends AbstractAlfrescoDistributedIT
{
    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(3, getSimpleClassName(), getProperties());
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }
    
    @Test
    public void testShardId() throws Exception
    {
        putHandleDefaults();

        int numAcls = 25;
        AclChangeSet bulkAclChangeSet = getAclChangeSet(numAcls);

        List<Acl> bulkAcls = new ArrayList<>();
        List<AclReaders> bulkAclReaders = new ArrayList<>();


        for (int i = 0; i < numAcls; i++) {
            Acl bulkAcl = getAcl(bulkAclChangeSet);
            bulkAcls.add(bulkAcl);
            bulkAclReaders.add(getAclReaders(bulkAclChangeSet,
                    bulkAcl,
                    Collections.singletonList("king" + bulkAcl.getId()),
                    Collections.singletonList("king" + bulkAcl.getId()),
                    null));
        }

        indexAclChangeSet(bulkAclChangeSet, bulkAcls, bulkAclReaders);

        int numNodes = 1000;
        List<Node> nodes = new ArrayList<>();
        List<NodeMetaData> nodeMetaDatas = new ArrayList<>();

        Transaction bigTxn = getTransaction(0, numNodes);

        for (int i = 0; i < numNodes; i++) {
            int aclIndex = i % numAcls;
            Node node = getNode(bigTxn, bulkAcls.get(aclIndex), Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, bulkAcls.get(aclIndex), "king", null, false);
            boolean even = i % 2 == 0; // if its even put it on shard 1 otherwise put it on shard 0.
            node.setShardPropertyValue(even?"1":"0");
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);

        Query contentQuery = new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world"));
        Query aclQuery = new TermQuery(new Term(FIELD_DOC_TYPE, SolrInformationServer.DOC_TYPE_ACL));
        Collection<SolrCore> shards = getCores(solrShards);
        List<SolrClient> shardClients = getShardedClients();
        long begin = System.currentTimeMillis();

        //Acls go to all cores
        waitForDocCountAllCores(aclQuery, numAcls, 25000);

        //Should be 1000
        waitForShardsCount(contentQuery,numNodes,20000, begin);
        begin = System.currentTimeMillis();

        for (int i = 0; i < shardClients.size(); ++i)
        {
            final int shardId = i;
            SolrCore core = shards.stream()
                    .filter(solrcore -> solrcore.getName().endsWith("" + shardId)).findAny().orElseThrow(RuntimeException::new);

            SolrClient client = shardClients.get(i);
            switch (core.getName())
            {
                case "shard0":
                case "shard1":
                    waitForDocCountCore(client, luceneToSolrQuery(contentQuery), 500, 1000, begin);
                    break;
                default:
                    //ignore other shards because we will check below
            }
        }

        //lets make sure the other nodes don't have any.
        assertShardCount(2, contentQuery, 0);

        Transaction txn1 = getTransaction(0, 2);
        List<Node> extraNodes = new ArrayList<>();
        List<NodeMetaData> extraNodeMetaDatas = new ArrayList<>();

        //Add a node that will get indexed by fallback to DBID
        Node node = getNode(txn1, bulkAcls.get(1), Node.SolrApiNodeStatus.UPDATED);
        extraNodes.add(node);
        NodeMetaData nodeMetaData = getNodeMetaData(node, txn1, bulkAcls.get(1), "king", null, false);
        node.setShardPropertyValue("node YOU DON'T");
        extraNodeMetaDatas.add(nodeMetaData);

        //Add another node that will get indexed by fallback to DBID
        node = getNode(txn1, bulkAcls.get(2), Node.SolrApiNodeStatus.UPDATED);
        extraNodes.add(node);
        nodeMetaData = getNodeMetaData(node, txn1, bulkAcls.get(2), "king", null, false);
        //Don't set the Share Property but add it anyway
        extraNodeMetaDatas.add(nodeMetaData);

        indexTransaction(txn1, extraNodes, extraNodeMetaDatas);

        begin = System.currentTimeMillis();
        //Asserts the the two nodes were not lost even though the ShardPropertyValue was incorrect
        waitForShardsCount(contentQuery,numNodes+2,30000, begin);
    }

    protected static Properties getProperties()
    {
        Properties prop = new Properties();
        prop.put("shard.method", ShardMethodEnum.EXPLICIT_ID.toString());
        //Normally this would be used by the Solr client which will automatically add the property to the node.shardPropertyValue
        //For testing this doesn't work like that so I setShardPropertyValue explicitly above.
        prop.put(SHARD_KEY_KEY, ContentModel.PROP_SKYPE.toString());
        return prop;
    }
}
