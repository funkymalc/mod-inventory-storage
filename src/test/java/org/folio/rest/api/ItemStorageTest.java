package org.folio.rest.api;

import static org.folio.rest.api.StorageTestSuite.itemsUrl;
import static org.folio.rest.api.StorageTestSuite.loanTypesUrl;
import static org.folio.rest.api.StorageTestSuite.materialTypesUrl;
import static org.folio.rest.support.JsonObjectMatchers.hasSoleMessgeContaining;
import static org.folio.rest.support.JsonObjectMatchers.validationErrorMatches;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.folio.rest.support.AdditionalHttpStatusCodes;
import org.folio.rest.support.JsonArrayHelper;
import org.folio.rest.support.JsonErrorResponse;
import org.folio.rest.support.JsonResponse;
import org.folio.rest.support.Response;
import org.folio.rest.support.ResponseHandler;
import org.folio.rest.support.TextResponse;
import org.folio.rest.support.client.LoanTypesClient;
import org.folio.rest.support.client.MaterialTypesClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ItemStorageTest extends TestBase {

  private static String journalMaterialTypeID;
  private static String booklMaterialTypeID;
  private static String videolMaterialTypeID;
  private static String canCirculateLoanTypeID;

  @BeforeClass
  public static void beforeAny() throws Exception {
    journalMaterialTypeID = new MaterialTypesClient(client, materialTypesUrl()).create("journal");
    booklMaterialTypeID = new MaterialTypesClient(client, materialTypesUrl()).create("book");
    videolMaterialTypeID = new MaterialTypesClient(client, materialTypesUrl()).create("video");
    canCirculateLoanTypeID = new LoanTypesClient(client, loanTypesUrl()).create("Can Circulate");
  }

  @Before
  public void beforeEach() throws MalformedURLException {
    StorageTestSuite.deleteAll(itemsUrl());
  }

  @After
  public void checkIdsAfterEach() {
    StorageTestSuite.checkForMismatchedIDs("item");
  }

  @Test
  public void canCreateAnItemViaCollectionResource()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();

    JsonObject itemToCreate = nod(id, instanceId);

    CompletableFuture<JsonResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
      ResponseHandler.json(createCompleted));

    JsonResponse postResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(postResponse.getStatusCode(), is(HttpURLConnection.HTTP_CREATED));

    JsonObject itemFromPost = postResponse.getJson();

    assertThat(itemFromPost.getString("id"), is(id.toString()));
    assertThat(itemFromPost.getString("instanceId"), is(instanceId.toString()));
    assertThat(itemFromPost.getString("title"), is("Nod"));
    assertThat(itemFromPost.getString("barcode"), is("565578437802"));
    assertThat(itemFromPost.getJsonObject("status").getString("name"),
      is("Available"));
    assertThat(itemFromPost.getString("materialTypeId"),
      is(journalMaterialTypeID));
    assertThat(itemFromPost.getString("permanentLoanTypeId"),
      is(canCirculateLoanTypeID));
    assertThat(itemFromPost.getJsonObject("location").getString("name"),
      is("Main Library"));

    JsonResponse getResponse = getById(id);

    assertThat(getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject itemFromGet = getResponse.getJson();

    assertThat(itemFromGet.getString("id"), is(id.toString()));
    assertThat(itemFromGet.getString("instanceId"), is(instanceId.toString()));
    assertThat(itemFromGet.getString("title"), is("Nod"));
    assertThat(itemFromGet.getString("barcode"), is("565578437802"));
    assertThat(itemFromGet.getJsonObject("status").getString("name"),
      is("Available"));
    assertThat(itemFromGet.getString("materialTypeId"),
      is(journalMaterialTypeID));
    assertThat(itemFromGet.getString("permanentLoanTypeId"),
      is(canCirculateLoanTypeID));
    assertThat(itemFromGet.getJsonObject("location").getString("name"),
      is("Main Library"));
  }

  @Test
  public void canCreateAnItemWithMinimalProperties()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();

    JsonObject itemToCreate = new JsonObject()
      .put("id", id.toString())
      .put("materialTypeId", journalMaterialTypeID)
      .put("permanentLoanTypeId", canCirculateLoanTypeID)
      .put("title", "Nod");

    CompletableFuture<JsonResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
      ResponseHandler.json(createCompleted));

    JsonResponse postResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(String.format("Failed to create item: %s", postResponse.getBody()),
      postResponse.getStatusCode(), is(HttpURLConnection.HTTP_CREATED));

    JsonObject itemFromPost = postResponse.getJson();

    assertThat(itemFromPost.getString("id"), is(id.toString()));

    JsonResponse getResponse = getById(id);

    assertThat(getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject itemFromGet = getResponse.getJson();

    assertThat(itemFromGet.getString("id"), is(id.toString()));
  }

  @Test
  public void canCreateAnItemWithoutProvidingID()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID instanceId = UUID.randomUUID();

    JsonObject itemToCreate = nod(null, instanceId);

    CompletableFuture<JsonResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
      ResponseHandler.json(createCompleted));

    JsonResponse postResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(postResponse.getStatusCode(), is(HttpURLConnection.HTTP_CREATED));

    JsonObject itemFromPost = postResponse.getJson();

    String newId = itemFromPost.getString("id");

    assertThat(newId, is(notNullValue()));

    JsonResponse getResponse = getById(UUID.fromString(newId));

    assertThat(getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject itemFromGet = getResponse.getJson();

    assertThat(itemFromGet.getString("id"), is(newId));
    assertThat(itemFromGet.getString("instanceId"), is(instanceId.toString()));
    assertThat(itemFromGet.getString("title"), is("Nod"));
    assertThat(itemFromGet.getString("barcode"), is("565578437802"));
    assertThat(itemFromGet.getJsonObject("status").getString("name"),
      is("Available"));
    assertThat(itemFromGet.getString("materialTypeId"),
      is(journalMaterialTypeID));
    assertThat(itemFromGet.getString("permanentLoanTypeId"),
      is(canCirculateLoanTypeID));
    assertThat(itemFromGet.getJsonObject("location").getString("name"),
      is("Main Library"));
  }

  @Test
  public void cannotCreateAnItemWithBlankTitle()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();

    JsonObject itemToCreate = new JsonObject()
      .put("id", id.toString())
      .put("materialTypeId", journalMaterialTypeID)
      .put("permanentLoanTypeId", canCirculateLoanTypeID)
      .put("title", "");

    CompletableFuture<JsonResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
      ResponseHandler.json(createCompleted));

    JsonResponse postResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(postResponse.getStatusCode(), is(AdditionalHttpStatusCodes.UNPROCESSABLE_ENTITY));

    List<JsonObject> errors = JsonArrayHelper.toList(
      postResponse.getJson().getJsonArray("errors"));

    assertThat(errors.size(), is(1));
    assertThat(errors, hasItem(
      validationErrorMatches("size must be between 1 and 255", "title")));
  }

  @Test
  public void cannotCreateAnItemWithTitleOver255Characters()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();

    JsonObject itemToCreate = new JsonObject()
      .put("id", id.toString())
      .put("materialTypeId", journalMaterialTypeID)
      .put("permanentLoanTypeId", canCirculateLoanTypeID)
      .put("title", String.join("", Collections.nCopies(256, "X")));

    CompletableFuture<JsonResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
      ResponseHandler.json(createCompleted));

    JsonResponse postResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(postResponse.getStatusCode(), is(AdditionalHttpStatusCodes.UNPROCESSABLE_ENTITY));

    List<JsonObject> errors = JsonArrayHelper.toList(
      postResponse.getJson().getJsonArray("errors"));

    assertThat(errors.size(), is(1));
    assertThat(errors, hasItem(
      validationErrorMatches("size must be between 1 and 255", "title")));
  }

  @Test
  public void cannotCreateAnItemWithIDThatIsNotUUID()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    String id = "1234";
    UUID instanceId = UUID.randomUUID();

    JsonObject itemToCreate = new JsonObject();

    itemToCreate.put("id", id.toString());
    itemToCreate.put("instanceId", instanceId.toString());
    itemToCreate.put("title", "Nod");
    itemToCreate.put("barcode", "565578437802");
    itemToCreate.put("status", new JsonObject().put("name", "Available"));
    itemToCreate.put("materialTypeId", journalMaterialTypeID);
    itemToCreate.put("permanentLoanTypeId", canCirculateLoanTypeID);
    itemToCreate.put("location", new JsonObject().put("name", "Main Library"));

    CompletableFuture<TextResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
      ResponseHandler.text(createCompleted));

    TextResponse postResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(postResponse.getStatusCode(), is(HttpURLConnection.HTTP_BAD_REQUEST));

    assertThat(postResponse.getBody(), is("invalid input syntax for uuid: \"1234\""));
  }

  @Test
  public void cannotCreateAnItemWithoutMaterialType()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();

    JsonObject itemToCreate = new JsonObject();

    itemToCreate.put("id", id.toString());
    itemToCreate.put("title", "Nod");
    itemToCreate.put("permanentLoanTypeId", canCirculateLoanTypeID);

    CompletableFuture<JsonResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
      ResponseHandler.json(createCompleted));

    JsonResponse postResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(postResponse.getStatusCode(), is(AdditionalHttpStatusCodes.UNPROCESSABLE_ENTITY));

    List<JsonObject> errors = JsonArrayHelper.toList(
      postResponse.getJson().getJsonArray("errors"));

    assertThat(errors.size(), is(1));
    assertThat(errors, hasItem(
      validationErrorMatches("may not be null", "materialTypeId")));
  }

  @Test
  public void canCreateAnItemAtSpecificLocation()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();

    JsonObject itemToCreate = nod(id, instanceId);

    CompletableFuture<Response> createCompleted = new CompletableFuture();

    client.put(itemsUrl(String.format("/%s", id)), itemToCreate,
      StorageTestSuite.TENANT_ID, ResponseHandler.empty(createCompleted));

    Response putResponse = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    JsonResponse getResponse = getById(id);

    //PUT currently cannot return a response
    assertThat(getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject item = getResponse.getJson();

    assertThat(item.getString("id"), is(id.toString()));
    assertThat(item.getString("instanceId"), is(instanceId.toString()));
    assertThat(item.getString("title"), is("Nod"));
    assertThat(item.getString("barcode"), is("565578437802"));
    assertThat(item.getJsonObject("status").getString("name"),
      is("Available"));
    assertThat(item.getString("materialTypeId"),
      is(journalMaterialTypeID));
    assertThat(item.getString("permanentLoanTypeId"),
      is(canCirculateLoanTypeID));
    assertThat(item.getJsonObject("location").getString("name"),
      is("Main Library"));
  }

  @Test
  public void cannotProvideAdditionalPropertiesInItem()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    JsonObject requestWithAdditionalProperty = nod();

    requestWithAdditionalProperty.put("somethingAdditional", "foo");

    CompletableFuture<JsonErrorResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), requestWithAdditionalProperty,
      StorageTestSuite.TENANT_ID, ResponseHandler.jsonErrors(createCompleted));

    JsonErrorResponse response = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(response.getStatusCode(), is(AdditionalHttpStatusCodes.UNPROCESSABLE_ENTITY));
    assertThat(response.getErrors(), hasSoleMessgeContaining("Unrecognized field"));
  }

  @Test
  public void cannotProvideAdditionalPropertiesInItemStatus()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    JsonObject requestWithAdditionalProperty = nod();

    requestWithAdditionalProperty
      .put("status", new JsonObject().put("somethingAdditional", "foo"));

    CompletableFuture<JsonErrorResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), requestWithAdditionalProperty,
      StorageTestSuite.TENANT_ID, ResponseHandler.jsonErrors(createCompleted));

    JsonErrorResponse response = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(response.getStatusCode(), is(AdditionalHttpStatusCodes.UNPROCESSABLE_ENTITY));
    assertThat(response.getErrors(), hasSoleMessgeContaining("Unrecognized field"));
  }

  @Test
  public void cannotProvideAdditionalPropertiesInItemLocation()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    JsonObject requestWithAdditionalProperty = nod();

    requestWithAdditionalProperty
      .put("location", new JsonObject().put("somethingAdditional", "foo"));

    CompletableFuture<JsonErrorResponse> createCompleted = new CompletableFuture();

    client.post(itemsUrl(), requestWithAdditionalProperty,
      StorageTestSuite.TENANT_ID, ResponseHandler.jsonErrors(createCompleted));

    JsonErrorResponse response = createCompleted.get(5, TimeUnit.SECONDS);

    assertThat(response.getStatusCode(), is(AdditionalHttpStatusCodes.UNPROCESSABLE_ENTITY));
    assertThat(response.getErrors(), hasSoleMessgeContaining("Unrecognized field"));
  }

  @Test
  public void canReplaceAnItemAtSpecificLocation()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();

    JsonObject itemToCreate = smallAngryPlanet(id, instanceId);

    createItem(itemToCreate);

    JsonObject replacement = itemToCreate.copy();
      replacement.put("barcode", "125845734657");
      replacement.put("location",
        new JsonObject().put("name", "Annex Library"));

    CompletableFuture<Response> replaceCompleted = new CompletableFuture();

    client.put(itemsUrl(String.format("/%s", id)), replacement,
      StorageTestSuite.TENANT_ID, ResponseHandler.empty(replaceCompleted));

    Response putResponse = replaceCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    JsonResponse getResponse = getById(id);

    //PUT currently cannot return a response
    assertThat(getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject item = getResponse.getJson();

    assertThat(item.getString("id"), is(id.toString()));
    assertThat(item.getString("instanceId"), is(instanceId.toString()));
    assertThat(item.getString("title"), is("Long Way to a Small Angry Planet"));
    assertThat(item.getString("barcode"), is("125845734657"));
    assertThat(item.getJsonObject("status").getString("name"),
      is("Available"));
    assertThat(item.getString("materialTypeId"),
      is(journalMaterialTypeID));
    assertThat(item.getJsonObject("location").getString("name"),
      is("Annex Library"));
  }

  @Test
  public void canReplaceAnItemWithASingleQuoteInTheTitle()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    UUID id = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();

    JsonObject itemToCreate = createItemRequest(id, instanceId,
      "The Time Traveller's Wife", "036587275931");

    createItem(itemToCreate);

    JsonObject replacement = itemToCreate.copy();
    replacement.put("barcode", "036587275931");
    replacement.put("location",
      new JsonObject().put("name", "Annex Library"));

    CompletableFuture<Response> replaceCompleted = new CompletableFuture();

    client.put(itemsUrl(String.format("/%s", id)), replacement,
      StorageTestSuite.TENANT_ID, ResponseHandler.empty(replaceCompleted));

    Response putResponse = replaceCompleted.get(5, TimeUnit.SECONDS);

    assertThat(putResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    JsonResponse getResponse = getById(id);

    //PUT currently cannot return a response
    assertThat(getResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));

    JsonObject item = getResponse.getJson();

    assertThat(item.getString("id"), is(id.toString()));
    assertThat(item.getString("instanceId"), is(instanceId.toString()));
    assertThat(item.getString("title"), is("The Time Traveller's Wife"));
    assertThat(item.getString("barcode"), is("036587275931"));
    assertThat(item.getJsonObject("status").getString("name"),
      is("Available"));
    assertThat(item.getString("materialTypeId"),
      is(journalMaterialTypeID));
    assertThat(item.getJsonObject("location").getString("name"),
      is("Annex Library"));
  }

  @Test
  public void canDeleteAnItem() throws InterruptedException,
    MalformedURLException, TimeoutException, ExecutionException {

    UUID id = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();

    JsonObject itemToCreate = smallAngryPlanet(id, instanceId);

    createItem(itemToCreate);

    CompletableFuture<Response> deleteCompleted = new CompletableFuture();

    client.delete(itemsUrl(String.format("/%s", id)),
      StorageTestSuite.TENANT_ID, ResponseHandler.empty(deleteCompleted));

    Response deleteResponse = deleteCompleted.get(5, TimeUnit.SECONDS);

    assertThat(deleteResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<Response> getCompleted = new CompletableFuture();

    client.get(itemsUrl(String.format("/%s", id)),
      StorageTestSuite.TENANT_ID, ResponseHandler.empty(getCompleted));

    Response getResponse = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(getResponse.getStatusCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
  }

  @Test
  public void canPageAllItems()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    createItem(smallAngryPlanet());
    createItem(nod());
    createItem(uprooted());
    createItem(temeraire());
    createItem(interestingTimes());

    CompletableFuture<JsonResponse> firstPageCompleted = new CompletableFuture();
    CompletableFuture<JsonResponse> secondPageCompleted = new CompletableFuture();

    client.get(itemsUrl() + "?limit=3", StorageTestSuite.TENANT_ID,
      ResponseHandler.json(firstPageCompleted));

    client.get(itemsUrl() + "?limit=3&offset=3", StorageTestSuite.TENANT_ID,
      ResponseHandler.json(secondPageCompleted));

    JsonResponse firstPageResponse = firstPageCompleted.get(5, TimeUnit.SECONDS);
    JsonResponse secondPageResponse = secondPageCompleted.get(5, TimeUnit.SECONDS);

    assertThat(firstPageResponse.getStatusCode(), is(200));
    assertThat(secondPageResponse.getStatusCode(), is(200));

    JsonObject firstPage = firstPageResponse.getJson();
    JsonObject secondPage = secondPageResponse.getJson();

    JsonArray firstPageItems = firstPage.getJsonArray("items");
    JsonArray secondPageItems = secondPage.getJsonArray("items");

    assertThat(firstPageItems.size(), is(3));
    assertThat(firstPage.getInteger("totalRecords"), is(5));

    assertThat(secondPageItems.size(), is(2));
    assertThat(secondPage.getInteger("totalRecords"), is(5));
  }

  @Test
  public void canSearchForItemsByTitle()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    createItem(smallAngryPlanet());
    createItem(nod());
    createItem(uprooted());
    createItem(temeraire());
    createItem(interestingTimes());

    CompletableFuture<JsonResponse> searchCompleted = new CompletableFuture();

    String url = itemsUrl() + "?query=title=\"*Up*\"";

    client.get(url,
      StorageTestSuite.TENANT_ID, ResponseHandler.json(searchCompleted));

    JsonResponse searchResponse = searchCompleted.get(5, TimeUnit.SECONDS);

    assertThat(searchResponse.getStatusCode(), is(200));

    JsonObject searchBody = searchResponse.getJson();

    JsonArray foundItems = searchBody.getJsonArray("items");

    assertThat(foundItems.size(), is(1));
    assertThat(searchBody.getInteger("totalRecords"), is(1));

    assertThat(foundItems.getJsonObject(0).getString("title"),
      is("Uprooted"));
  }

  @Test
  public void canSearchForItemsByBarcode()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    createItem(smallAngryPlanet());
    createItem(nod());
    createItem(uprooted());
    createItem(temeraire());
    createItem(interestingTimes());

    CompletableFuture<JsonResponse> searchCompleted = new CompletableFuture();

    String url = itemsUrl() + "?query=barcode=036000291452";

    client.get(url,
      StorageTestSuite.TENANT_ID, ResponseHandler.json(searchCompleted));

    JsonResponse searchResponse = searchCompleted.get(5, TimeUnit.SECONDS);

    assertThat(searchResponse.getStatusCode(), is(200));

    JsonObject searchBody = searchResponse.getJson();

    JsonArray foundItems = searchBody.getJsonArray("items");

    assertThat(foundItems.size(), is(1));
    assertThat(searchBody.getInteger("totalRecords"), is(1));

    assertThat(foundItems.getJsonObject(0).getString("title"),
      is("Long Way to a Small Angry Planet"));
  }

  @Test
  public void cannotSearchForItemsUsingADefaultField()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    createItem(smallAngryPlanet());
    createItem(nod());
    createItem(uprooted());
    createItem(temeraire());
    createItem(interestingTimes());

    CompletableFuture<TextResponse> searchCompleted = new CompletableFuture();

    String url = itemsUrl() + "?query=t";

    client.get(url,
      StorageTestSuite.TENANT_ID, ResponseHandler.text(searchCompleted));

    TextResponse searchResponse = searchCompleted.get(5, TimeUnit.SECONDS);

    assertThat(searchResponse.getStatusCode(), is(500));

    String error = searchResponse.getBody();

    assertThat(error,
      is("CQL State Error for 't': org.z3950.zing.cql.cql2pgjson.QueryValidationException: cql.serverChoice requested, but no serverChoiceIndexes defined."));
  }

  @Test
  public void canDeleteAllItems()
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    createItem(smallAngryPlanet());
    createItem(nod());
    createItem(uprooted());
    createItem(temeraire());
    createItem(interestingTimes());

    CompletableFuture<Response> deleteAllFinished = new CompletableFuture();

    client.delete(itemsUrl(), StorageTestSuite.TENANT_ID,
      ResponseHandler.empty(deleteAllFinished));

    Response deleteResponse = deleteAllFinished.get(5, TimeUnit.SECONDS);

    assertThat(deleteResponse.getStatusCode(), is(HttpURLConnection.HTTP_NO_CONTENT));

    CompletableFuture<JsonResponse> getCompleted = new CompletableFuture();

    client.get(itemsUrl(), StorageTestSuite.TENANT_ID,
      ResponseHandler.json(getCompleted));

    JsonResponse response = getCompleted.get(5, TimeUnit.SECONDS);

    JsonObject responseBody = response.getJson();

    JsonArray allItems = responseBody.getJsonArray("items");

    assertThat(allItems.size(), is(0));
    assertThat(responseBody.getInteger("totalRecords"), is(0));
  }

  @Test
  public void tenantIsRequiredForCreatingNewItem()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    CompletableFuture<TextResponse> postCompleted = new CompletableFuture();

    client.post(itemsUrl(), smallAngryPlanet(),
      ResponseHandler.text(postCompleted));

    TextResponse response = postCompleted.get(5, TimeUnit.SECONDS);

    assertThat(response.getStatusCode(), is(400));
    assertThat(response.getBody(), is("Unable to process request Tenant must be set"));
  }

  @Test
  public void tenantIsRequiredForGettingAnItem()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    URL getInstanceUrl = itemsUrl(String.format("/%s",
      UUID.randomUUID().toString()));

    CompletableFuture<TextResponse> getCompleted = new CompletableFuture();

    client.get(getInstanceUrl, ResponseHandler.text(getCompleted));

    TextResponse response = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(response.getStatusCode(), is(400));
    assertThat(response.getBody(), is("Unable to process request Tenant must be set"));
  }

  @Test
  public void tenantIsRequiredForGettingAllItems()
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    CompletableFuture<TextResponse> getCompleted = new CompletableFuture();

    client.get(itemsUrl(), ResponseHandler.text(getCompleted));

    TextResponse response = getCompleted.get(5, TimeUnit.SECONDS);

    assertThat(response.getStatusCode(), is(400));
    assertThat(response.getBody(), is("Unable to process request Tenant must be set"));
  }

  @Test
  public void testCrossTableQueries() throws Exception {
    String url = itemsUrl() + "?query=";

    createItem(createItemRequest(UUID.randomUUID(), UUID.randomUUID(),
      "A journal title", "036000291452", journalMaterialTypeID));
    createItem(createItemRequest(UUID.randomUUID(), UUID.randomUUID(),
      "A book title", "036000291452", booklMaterialTypeID));
    createItem(createItemRequest(UUID.randomUUID(), UUID.randomUUID(),
      "A video title", "036000291452", videolMaterialTypeID));

    //query on item and sort by material type
    String url1 = url+URLEncoder.encode("title=title sortBy materialType.name/sort.descending", "UTF-8");
    String url2 = url+URLEncoder.encode("title=title sortBy materialType.name/sort.ascending", "UTF-8");
    //query and sort on material type via items end point
    String url3 = url+URLEncoder.encode("materialType.name=Journal* sortBy materialType.name/sort.descending", "UTF-8");
    //query on item sort on item and material type
    String url4 = url+URLEncoder.encode("title=title sortby materialType.name title", "UTF-8");
    //query on item and material type sort by material type
    String url5 = url+URLEncoder.encode("title=title and materialType.name=Journal* sortby materialType.name", "UTF-8");
    //query on item and sort by item
    String url6 = url+URLEncoder.encode("title=abc sortBy materialType.name", "UTF-8");
    //non existant material type - 0 results
    String url7 = url+URLEncoder.encode("title=title and materialType.name=abc* sortby materialType.name", "UTF-8");

    String url8 = url+URLEncoder.encode("materialType="+videolMaterialTypeID, "UTF-8");

    CompletableFuture<JsonResponse> cqlCF1 = new CompletableFuture();
    CompletableFuture<JsonResponse> cqlCF2 = new CompletableFuture();
    CompletableFuture<JsonResponse> cqlCF3 = new CompletableFuture();
    CompletableFuture<JsonResponse> cqlCF4 = new CompletableFuture();
    CompletableFuture<JsonResponse> cqlCF5 = new CompletableFuture();
    CompletableFuture<JsonResponse> cqlCF6 = new CompletableFuture();
    CompletableFuture<JsonResponse> cqlCF7 = new CompletableFuture();
    CompletableFuture<JsonResponse> cqlCF8 = new CompletableFuture();

    String[] urls = new String[]{url1, url2, url3, url4, url5, url6, url7, url8};
    CompletableFuture<JsonResponse>[] cqlCF = new CompletableFuture[]{cqlCF1, cqlCF2, cqlCF3, cqlCF4, cqlCF5, cqlCF6, cqlCF7, cqlCF8};

    for(int i=0; i<8; i++){
      CompletableFuture<JsonResponse> cf = cqlCF[i];
      String cqlURL = urls[i];
      client.get(cqlURL, StorageTestSuite.TENANT_ID, ResponseHandler.json(cf));

      JsonResponse cqlResponse = cf.get(5, TimeUnit.SECONDS);
      assertThat(cqlResponse.getStatusCode(), is(HttpURLConnection.HTTP_OK));
      System.out.println(cqlResponse.getBody() +
        "\nStatus - " + cqlResponse.getStatusCode() + " at " + System.currentTimeMillis() + " for " + cqlURL);

      if(i==6){
        assertThat(0, is(cqlResponse.getJson().getInteger("totalRecords")));
      } else if(i==5){
        assertThat(0, is(cqlResponse.getJson().getInteger("totalRecords")));
      } else if(i==4){
        assertThat(1, is(cqlResponse.getJson().getInteger("totalRecords")));
      } else if(i==0){
        assertThat("A video title" , is(cqlResponse.getJson().getJsonArray("items").getJsonObject(0).getString("title")));
      }else if(i==1){
        assertThat("A book title" , is(cqlResponse.getJson().getJsonArray("items").getJsonObject(0).getString("title")));
      }else if(i==2){
        assertThat(1, is(cqlResponse.getJson().getInteger("totalRecords")));
      }
    }
  }

  private JsonResponse getById(UUID id)
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    URL getItemUrl = itemsUrl(String.format("/%s", id));

    CompletableFuture<JsonResponse> getCompleted = new CompletableFuture();

    client.get(getItemUrl, StorageTestSuite.TENANT_ID,
      ResponseHandler.json(getCompleted));

    return getCompleted.get(5, TimeUnit.SECONDS);
  }

  private void createItem(JsonObject itemToCreate)
    throws MalformedURLException, InterruptedException,
    ExecutionException, TimeoutException {

    CompletableFuture<TextResponse> createCompleted = new CompletableFuture();

    try {
      client.post(itemsUrl(), itemToCreate, StorageTestSuite.TENANT_ID,
        ResponseHandler.text(createCompleted));

      TextResponse response = createCompleted.get(2, TimeUnit.SECONDS);

      if (response.getStatusCode() != 201) {
        System.out.println("WARNING!!!!! Create item preparation failed: "
          + response.getBody());
      }
    }
    catch(Exception e) {
      System.out.println("WARNING!!!!! Create item preparation failed: "
        + e.getMessage());
    }
  }

  private JsonObject createItemRequest(
      UUID id,
      UUID instanceId,
      String title,
      String barcode) {
    return createItemRequest(id, instanceId, title, barcode, journalMaterialTypeID);
  }

  private JsonObject createItemRequest(
    UUID id,
    UUID instanceId,
    String title,
    String barcode,
    String materialType) {

    JsonObject itemToCreate = new JsonObject();

    if(id != null) {
      itemToCreate.put("id", id.toString());
    }

    itemToCreate.put("instanceId", instanceId.toString());
    itemToCreate.put("title", title);
    itemToCreate.put("barcode", barcode);
    itemToCreate.put("status", new JsonObject().put("name", "Available"));
    itemToCreate.put("materialTypeId", materialType);
    itemToCreate.put("permanentLoanTypeId", canCirculateLoanTypeID);
    itemToCreate.put("location", new JsonObject().put("name", "Main Library"));

    return itemToCreate;
  }

  private JsonObject smallAngryPlanet(UUID itemId, UUID instanceId) {
    return createItemRequest(itemId, instanceId,
      "Long Way to a Small Angry Planet", "036000291452");
  }

  private JsonObject smallAngryPlanet() {
    return smallAngryPlanet(UUID.randomUUID(), UUID.randomUUID());
  }

  private JsonObject nod(UUID itemId, UUID instanceId) {
    return createItemRequest(itemId, instanceId,
      "Nod", "565578437802");
  }

  private JsonObject nod() {
    return nod(UUID.randomUUID(), UUID.randomUUID());
  }

  private JsonObject uprooted() {
    return createItemRequest(UUID.randomUUID(), UUID.randomUUID(),
      "Uprooted", "657670342075");
  }

  private JsonObject temeraire() {
    return createItemRequest(UUID.randomUUID(), UUID.randomUUID(),
      "Temeraire", "232142443432");
  }

  private JsonObject interestingTimes() {
    return createItemRequest(UUID.randomUUID(), UUID.randomUUID(),
      "Interesting Times", "56454543534");
  }
}
