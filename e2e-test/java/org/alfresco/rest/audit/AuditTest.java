package org.alfresco.rest.audit;

import static org.hamcrest.Matchers.is;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestAuditAppModel;
import org.alfresco.rest.model.RestAuditAppModelsCollection;
import org.alfresco.rest.model.RestAuditEntryModel;
import org.alfresco.rest.model.RestAuditEntryModelsCollection;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.JmxBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import com.jayway.restassured.RestAssured;

public abstract class AuditTest extends RestTest
{

    @Autowired
    protected RestWrapper restAPI;
    
    @Autowired
    protected JmxBuilder jmxBuilder;
    
    protected UserModel userModel,adminUser;
    protected RestAuditAppModelsCollection restAuditCollection;
    protected RestAuditAppModel restAuditAppModel;
    protected RestAuditEntryModel restAuditEntryModel;
    protected RestAuditEntryModelsCollection restAuditEntryCollection;
    protected RestAuditAppModel syncRestAuditAppModel;
    protected RestAuditAppModel taggingRestAuditAppModel;
    

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        //Using two users, because audit API is designed for users with admin rights.
        userModel = dataUser.createRandomTestUser();
        adminUser=dataUser.getAdminUser();
        //GET /alfresco/service/api/audit/control to verify if Audit is enabled on the system.
        RestAssured.basePath = "";
        restAPI.configureRequestSpec().setBasePath(RestAssured.basePath);
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "alfresco/service/api/audit/control");
        RestResponse response = restAPI.authenticateUser(adminUser).process(request);
        response.assertThat().body("enabled", is(true));
        //GET /audit-applications and verify that there are audit applications in the system.
        restAuditCollection = restClient.authenticateUser(adminUser).withCoreAPI().usingAudit().getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditCollection.assertThat().entriesListIsNotEmpty();

        //enable alfresco-access audit application
        jmxBuilder.getJmxClient().writeProperty("Alfresco:Type=Configuration,Category=Audit,id1=default", "audit.alfresco-access.enabled", Boolean.TRUE.toString());
        String alfrescoAccessEnabled = jmxBuilder.getJmxClient().readProperty("Alfresco:Type=Configuration,Category=Audit,id1=default", "audit.alfresco-access.enabled").toString();
        Assert.assertEquals(alfrescoAccessEnabled, Boolean.TRUE.toString(), String.format("Property audit.alfresco-access.enabled is [%s]", alfrescoAccessEnabled));

        //
        int i=0;
        do
        {
            restAuditAppModel = restAuditCollection.getEntries().get(i++).onModel();
        }while (!restAuditAppModel.getName().equals("alfresco-access"));

    }
    
    protected RestAuditAppModel getSyncRestAuditAppModel(UserModel userModel) throws Exception
    {
	    restAuditCollection = restClient.authenticateUser(userModel).withCoreAPI().usingAudit().getAuditApplications();
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    restAuditCollection.assertThat().entriesListIsNotEmpty();
	    RestAuditAppModel syncRestAuditAppModel = restAuditCollection.getEntries().get(0).onModel();
		return syncRestAuditAppModel;
    }
    
    protected RestAuditAppModel getTaggingRestAuditAppModel(UserModel userModel) throws Exception
    {
	    restAuditCollection = restClient.authenticateUser(userModel).withCoreAPI().usingAudit().getAuditApplications();
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    restAuditCollection.assertThat().entriesListIsNotEmpty();
	    RestAuditAppModel taggingRestAuditAppModel = restAuditCollection.getEntries().get(1).onModel();
	    return taggingRestAuditAppModel;
    }

}
