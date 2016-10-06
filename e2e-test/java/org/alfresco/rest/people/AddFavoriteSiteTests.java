package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestPeopleApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "people", "sanity" })
public class AddFavoriteSiteTests extends RestTest
{
    @Autowired
    RestPeopleApi peopleApi;

    UserModel userModel;
    SiteModel siteModel;
    UserModel searchedUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();

        peopleApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify manager user add a favorite site with Rest API and response is successful (201)")
    public void managerUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);

        restClient.authenticateUser(managerUser);
        peopleApi.addFavoriteSite(managerUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify collaborator user add a favorite site with Rest API and response is successful (201)")
    public void collaboratorUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);

        restClient.authenticateUser(collaboratorUser);
        peopleApi.addFavoriteSite(collaboratorUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify contributor user add a favorite site with Rest API and response is successful (201)")
    public void contributorUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);

        restClient.authenticateUser(contributorUser);
        peopleApi.addFavoriteSite(contributorUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify consumer user add a favorite site with Rest API and response is successful (201)")
    public void consumerUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);

        restClient.authenticateUser(consumerUser);
        peopleApi.addFavoriteSite(consumerUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify admin user add a favorite site with Rest API and response is successful (201)")
    public void adminUserAddFavoriteSiteWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();

        restClient.authenticateUser(adminUser);
        peopleApi.addFavoriteSite(adminUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }

    @Bug(id = "")
    @TestRail(section = { "rest-api", "people" }, executionType = ExecutionType.SANITY, description = "Verify a manager user is NOT Authorized to add a favorite site with Rest API when authentication fails (401)")
    public void managerUserNotAuthorizedFailsToAddFavoriteSite() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        managerUser.setPassword("newpassword");

        restClient.authenticateUser(managerUser);
        peopleApi.addFavoriteSite(managerUser, siteModel);
        peopleApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
