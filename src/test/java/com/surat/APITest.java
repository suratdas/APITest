package com.surat;

import java.util.List;

import groovy.time.BaseDuration.From;

import org.junit.Test;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static com.jayway.restassured.module.jsv.JsonSchemaValidator.*;

public class APITest {
	public APITest(){
		//Server variables
		//baseURI ="http://api.openweathermap.org/data/2.5/weather?q=london";
		baseURI ="https://api.github.com/users/mralexgray/repos";
		//port=8080;
		//authentication = basic("username", "password");
	}

	@Test
	public void test() {

		//Add these 3 jar files from rest-assured website if you don't want to use maven. This project uses maven
		//1. rest-assured-2.4.1-dist  2. json-path-2.4.1-dist  3. json-schema-validator-2.4.1-dist   

		//get the schema generated at http://jsonschema.net/#/   
		//Also, if you want to compare the schema for the whole array, give just one object from the array and then click "Single schema (list validation)". 
		//If an item takes more than one value (example, either null or string) edit the schema as "type": ["string","null"] 
		get().then().body(matchesJsonSchemaInClasspath("json-schema.json"));

		//Logging the response
		get().then().log().all(); //Takes cookies/body/All.

		//Assertion comparisons
		get().then().body("id", hasItem(16160992)); //Will throw an exception if condition not met, no output if its met.
		get().then().body("owner.login", hasItem("mralexgray")); //Will throw an exception if condition not met, no output if its met.

		//Extracting particular data
		System.out.println(get().path("owner.login"));
		Response response = get();
		ResponseBody<?> responsebody = response.body();
		System.out.println("Status code:"+response.getStatusCode() + " Status line: "+response.statusLine());
		System.out.println(responsebody.path("id"));		
		System.out.println(responsebody.path("owner.login.size()"));

		//Extracting response another way
		JsonPath jsonPath = new JsonPath(get().asString());
		List<Integer> winnerIds = jsonPath.get("id");
		for(Integer temp : winnerIds){
			System.out.println(temp);
		}
		//*/

		//Asserts count of the elements
		expect().body("owner.login.size()",equalTo(30)).when().get();
		
	}

}