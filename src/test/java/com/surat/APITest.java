package com.surat;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class APITest {

	private Response response;
	private String endpoint;
	private Map<String, String> headersMap = new HashMap<String, String>();
	private String xml;

	public APITest() {
		//Server variables
		//baseURI ="http://api.openweathermap.org/data/2.5/weather?q=london";
		baseURI = "https://api.github.com/users/suratdas/repos";
		//port=8080;
		//authentication = basic("username", "password");
	}


	@Test
	public void TestCase1() {
		SetEndpoint("https://api.github.com/users/suratdas/repos");
		SetHeaders("Accept:application/json");
		Assert.assertEquals(200, GETRequestStatusCode());
		Assert.assertEquals(5, GetNodeArray("id").size());
	}

	@Test
	public void TestCase2() {

		//Add these 3 jar files from rest-assured website if you don't want to use maven. This project uses maven
		//1. rest-assured-2.4.1-dist  2. json-path-2.4.1-dist  3. json-schema-validator-2.4.1-dist

		//get the schema generated at http://jsonschema.net/#/
		//Also, if you want to compare the schema for the whole array, give just one object from the array and then click "Single schema (list validation)".
		//If an item takes more than one value (example, either null or string) edit the schema as "type": ["string","null"]
		get().then().body(matchesJsonSchemaInClasspath("json-schema.json"));

		//Logging the response
		get().then().log().all(); //Takes cookies/body/All.

		//Assertion comparisons
		get().then().body("id", hasItem(25268033)); //Will throw an exception if condition not met, no output if its met.
		get().then().body("owner.login", hasItem("suratdas")); //Will throw an exception if condition not met, no output if its met.

		//Extracting particular data
		System.out.println(get().path("owner.login").toString());
		Response response = get();
		ResponseBody<?> responsebody = response.body();
		System.out.println("Status code:" + response.getStatusCode() + " Status line: " + response.statusLine());
		System.out.println(responsebody.path("id").toString());
		System.out.println(responsebody.path("owner.login.size()").toString());

		//Extracting response another way
		JsonPath jsonPath = new JsonPath(get().asString());
		List<Integer> winnerIds = jsonPath.get("id");
		for (Integer temp : winnerIds) {
			System.out.println(temp);
		}
		//*/

		//Asserts count of the elements
		expect().body("owner.login.size()", equalTo(5)).when().get();

	}


	public boolean SetEndpoint(String temp) {
		endpoint = temp;
		return true;
	}

	public boolean SetHeaders(String headersPassed) {
		xml = "";
		List<String> headersStringList = Arrays.asList(headersPassed.split("\n"));
		headersStringList.forEach(header -> {
			String temp = header;
			if (header.contains("application/xml")) xml = "xml";
			headersMap.put(temp.split(":")[0].trim(), temp.replace(temp.split(":")[0] + ":", "").trim());
            /*headersList.add(header.split(":")[0].trim());
            headersList.add(header.replace(header.split(":")[0] + ":", "").trim());*/
		});

		return true;
	}

	public int GETRequestStatusCode() {

		//Clean up previous response
		if (response != null) response = delete();

		baseURI = endpoint.trim();
		RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
		requestSpecBuilder.addHeaders(headersMap);
		response = given().spec(requestSpecBuilder.build()).when().get();
		response.getBody().prettyPrint();

		//Clean up the list for next request
		//headersList.clear();
		headersMap.clear();

		return response.getStatusCode();
	}

	public int GetStatusCode() {
		return response.getStatusCode();
	}


	public String GetNode(String nodeName) {
		try {
			if (xml.contains("xml")) {
				String returnValue = response.body().xmlPath().get().getNode(nodeName).value();
				return returnValue;
			}
			return response.body().jsonPath().get(nodeName).toString();
		} catch (Exception e) {
			return "";
		}
	}

	public List<String> GetNodeArray(String nodeName) {
		try {
			return response.body().jsonPath().get(nodeName);
		} catch (Exception e) {
			return null;
		}
	}

	public Map<String, String> GetResponseHeaders() {
		List<Header> headers = response.getHeaders().asList();
		Map<String, String> responseHeadersMap = new HashMap<>();
		headers.forEach(header -> responseHeadersMap.put(header.getName(), header.getValue()));
		return responseHeadersMap;
	}

	public Map<String, String> GetResponseCookies() {
		Map<String, String> cookies = response.getCookies();
		Set<String> keys = cookies.keySet();
		Map<String, String> responseCookiesMap = new HashMap<>();
		keys.forEach(key -> responseCookiesMap.put(key, cookies.get(key)));
		return responseCookiesMap;
	}

}