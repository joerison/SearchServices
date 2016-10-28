package org.alfresco.cmis.search;

import org.alfresco.cmis.CmisTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.QueryModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.CMIS, TestGroup.QUERIES })
public class SolrSearchInFolder extends CmisTest
{
    UserModel testUser;
    SiteModel testSite;
    FolderModel parentFolder, subFolder1, subFolder2, subFolder3;
    FileModel subFile1, subFile2, subFile3, subFile4, subFile5;
    XMLTestData testData;
    
    @BeforeClass(alwaysRun = true)
    public void deployCustomContentModel() throws Exception
    {
        XMLTestDataProvider.setXmlImputFile("src/main/resources/shared-resources/testdata/search-in-folder.xml");
        
        // create input data
        parentFolder = FolderModel.getRandomFolderModel();
        subFolder1 = FolderModel.getRandomFolderModel();
        subFolder2 = FolderModel.getRandomFolderModel();
        subFolder3 = new FolderModel("subFolder");
        subFile5 = new FileModel("fifthFile.txt",FileType.TEXT_PLAIN, "fifthFile content");
        subFile1 = new FileModel("firstFile", FileType.MSEXCEL);
        subFile2 = FileModel.getRandomFileModel(FileType.MSPOWERPOINT2007);
        subFile3 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        subFile4 = new FileModel("fourthFile", "fourthFileTitle", "fourthFileDescription", FileType.MSWORD2007);
        
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPublicRandomSite();
        cmisApi.authenticateUser(testUser).usingSite(testSite).createFolder(parentFolder)
            .then().usingResource(parentFolder)
                .createFile(subFile5).and().assertThat().existsInRepo().and().assertThat().contentIs("fifthFile content")
                .createFolder(subFolder1).and().assertThat().existsInRepo()
                .createFolder(subFolder2).and().assertThat().existsInRepo()
                .createFolder(subFolder3).and().assertThat().existsInRepo()
                .createFile(subFile1).and().assertThat().existsInRepo()
                .createFile(subFile2).and().assertThat().existsInRepo()
                .createFile(subFile3).and().assertThat().existsInRepo()
                .createFile(subFile4).and().assertThat().existsInRepo();
        Utility.waitToLoopTime(10);
    }
    
    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData")
    public void executeCMISQuery(QueryModel query)
    {
        String currentQuery = String.format(query.getValue(), parentFolder.getNodeRef());
        cmisApi.withQuery(currentQuery)
            .assertResultsCountIs(query.getResults());
    }
}
