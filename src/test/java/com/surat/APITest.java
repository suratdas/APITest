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
		baseURI ="https://api.github.com/users/<username>/repos";
		//port=8080;
		//authentication = basic("username", "password");
	}

	@Test
	public void JunitTest() {

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
		get().then().body("owner.login", hasItem("<username>")); //Will throw an exception if condition not met, no output if its met.

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
	
	public void RegularMethod() {
		//Define base url for the endpoint
	        baseURI ="https://api.github.com/users/<username>/";
	        //Get the response and put it in variables
	        Response response = get("repos");
	        ResponseBody responsebody = response.body();
	        //Get the status code
	        System.out.println("Status code:"+response.getStatusCode() + " Status line: "+response.statusLine()); //Prings => Status code:200 Status line: HTTP/1.1 200 OK
	        //Get the response headers
	        System.out.println("Headers:");
	        List<Header> headers = response.getHeaders().asList();
	        for(Header temp : headers) {
	            System.out.println("\t"+temp.getName() +":"+temp.getValue());
	        }
	        //Get the response cookies
	        Response response1 = get("http://google.com");
	        System.out.println("Cookie size : "+ response1.cookies().size());
	        Map<String,String> cookies = response1.getCookies();
	        Set<String> keys = cookies.keySet();
	        for(String key:keys){
	            System.out.println("\t"+key +"=>"+ cookies.get(key));
	        }
	        //Process body content : display whole body, array of nodes/single node or assert if it contains some value etc.
	        System.out.println("Body:\n\t"+responsebody.asString()); //Prints whle body
	        System.out.println("Example array from response: "+responsebody.path("id")); //prints array of id in the response
	        System.out.println("Calculate Size: "+responsebody.path("owner.login.size()")); //prints count of an array in response body
	        try {
	            response.then().body("id", hasItem(36698447)); //Will throw an exception if condition not met, no output if its met.
	            System.out.println("Found the item.");
	        }
	        catch(AssertionError e){
	            //Do whatever you want to do to when assertion fails
	            System.out.println("Exception seen:");
	            System.out.print("\t"+e.getMessage());
	        }
	        //Extracting response another way using JsonPath
	        JsonPath jsonPath = new JsonPath(responsebody.asString());
	        List<Integer> winnerIds = jsonPath.get("id");
	        System.out.println("Using JsonPath:");
	        System.out.print("\t");
	        for(Integer temp : winnerIds){
	            System.out.print(temp+", ");
	        }
	        //*/	
	}

}
