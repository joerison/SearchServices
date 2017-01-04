package org.alfresco.cmis.search;

import org.alfresco.cmis.CmisTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.QueryModel;
import org.alfresco.utility.model.TestGroup;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SolrSearchByPathTests extends CmisTest
{
    XMLTestData testData;

    @BeforeClass(alwaysRun = true)
    public void readTestDataFile()
    {
        cmisApi.authenticateUser(dataUser.getAdminUser());
    }

    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment() throws Exception
    {
        testData.cleanup(dataContent);
    }

    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getAllData")
    @XMLDataConfig(file = "src/main/resources/shared-resources/testdata/search-by-path.xml")
    public void prepareDataForSearchByPath(XMLTestData testData) throws Exception
    {
        this.testData = testData;
        testData.createUsers(dataUser);
        testData.createSitesStructure(dataSite, dataContent, dataUser);
        // wait for solr index
        Utility.waitToLoopTime(25);
    }

    @Test(groups = { TestGroup.CMIS, TestGroup.QUERIES },
            dependsOnMethods = "prepareDataForSearchByPath", dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData")
    @XMLDataConfig(file = "src/main/resources/shared-resources/testdata/search-by-path.xml")
    public void executeSearchByPathQueries(QueryModel query) throws Exception
    {
        cmisApi.withQuery(query.getValue()).assertResultsCountIs(query.getResults());
    }
}
