package com.surat;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;
import com.jayway.restassured.specification.RequestSpecification;
import com.sun.jndi.toolkit.url.Uri;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.util.*;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class APITest {

    private static final String DATABASE_DRIVER_CLASS = "";
    private static Connection connection;


    //Connection string examples, set the one corresponding to your database.

    // MySQL	jdbc:mysql://HOST/DATABASE
    //DATABASE_DRIVER_CLASS = com.mysql.jdbc.Driver

    //Postgresql	jdbc:postgresql://HOST/DATABASE
    //DATABASE_DRIVER_CLASS = org.postgresql.Driver

    //MS SQL Server    jdbc:microsoft:sqlserver://HOST:1433;DatabaseName=DATABASE <--- MS SQL Server 2000
    //jdbc:sqlserver://<HOST>:1433;databaseName=MyDB;integratedSecurity=true;  <--- MS SQL Server 2005 and newer
    //With integrated security    jdbc:sqlserver://<database>:<port>;databaseName=<databaseName>;integratedSecurity=true;
    //DATABASE_DRIVER_CLASS  = com.microsoft.jdbc.sqlserver.sqlserverdriver <--- MS SQL Server 2000 and older
    //DATABASE_DRIVER_CLASS = com.microsoft.sqlserver.jdbc.SQLServerDriver<--- MS SQL Server 2005 and newer

    //DB2	jdbc:as400://HOST/DATABASE;
    //DATABASE_DRIVER_CLASS = com.ibm.as400.access.AS400JDBCDriver

    private String endpoint;
    private Map<String, String> headersMap = new HashMap<>();
    private String body = "";
    String modifiedBodyForPUT = "";
    private LinkedHashMap<String, String> responseToDatabaseMap = new LinkedHashMap<>();
    private String contentTypeHeaderPassed;
    private Response response;
    int statusCode;
    String errors = "";

    //Authentication variables
    String access_token = "";
    private TreeMap<String, String> dbQueryResultSet;


    public APITest() {
        //Server variables
        baseURI = "http://api.openweathermap.org/data/2.5/weather?q=london";
        //baseURI = "https://api.github.com/users/suratdas/repos";
        //port=8080;
        //authentication = basic("username", "password");
    }


    @Test
    public void TestCase1() {
        setEndpoint("http://api.openweathermap.org/data/2.5/weather?q=london");
        setHeaders("Accept:application/json");
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

    public static TreeMap<String, String> GetResultFirstRow(String query, String databaseConnectionString) throws Exception {
        if (connection == null) {
            Class.forName(DATABASE_DRIVER_CLASS);
            connection = DriverManager.getConnection(databaseConnectionString);
        }

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        ResultSetMetaData metadata = resultSet.getMetaData();
        TreeMap<String, String> databaseResult = new TreeMap<>();
        if (resultSet != null) {
            int columnCount = metadata.getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(metadata.getColumnName(i) + ": " + resultSet.getString(i) + "\n");
                    databaseResult.put(metadata.getColumnName(i), resultSet.getString(i));
                }
                System.out.println("\n");
            }
        }
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
            }
        }
        return databaseResult;
    }


    public String errors() {
        return errors;
    }

    public long getCurrnetTime() {
        return System.currentTimeMillis();
    }

    public boolean setEndpoint(String temp) {
        endpoint = temp;
        return true;
    }

    public String getAuthenticationHeaderWithMethodAndURL(String requestMethodVerb, String requestUrl) throws Exception {
        requestMethodVerb = requestMethodVerb.trim();
        if (requestMethodVerb.length() < 3 || !(requestMethodVerb.equalsIgnoreCase("get") || requestMethodVerb.equalsIgnoreCase("post") || requestMethodVerb.equalsIgnoreCase("put") || requestMethodVerb.equalsIgnoreCase("delete"))) {
            errors += "You have not specified the right method name.\n";
            return "You have not specified the right method name.";
        }
        String macKey = GetNode("mac_key");
        String token = GetNode("access_token");
        String algorithmType = GetNode("mac_algorithm");
        if (macKey == null || token == null || macKey.isEmpty() || token.isEmpty()) {
            errors += "mac_key and access_token are not populated. You may have to authenticate first.\n";
            return "mac_key and access_token are not populated. You may have to authenticate first.";
        }
        String macExtension = "";
        char macNewLine = (char) 0x0A;
        Uri requestUri = new Uri(requestUrl);
        String hostName = requestUri.getHost();
        String port = Integer.toString(requestUri.getPort());
        String hostPort = port.contains("-1") ? "80" : port;
        if (requestUrl.trim().startsWith("https")) hostPort = "443";
        String timestamp = Long.toString(DateTime.now(DateTimeZone.UTC).toDateTime().getMillis() / 1000);
        String nonce = UUID.randomUUID().toString().substring(0, 8);
        String requestPathAndQuery = requestUri.getPath() + (requestUri.getQuery() == null ? "" : requestUri.getQuery());
        String normalized = timestamp + macNewLine + nonce + macNewLine + requestMethodVerb.toUpperCase() + macNewLine + requestPathAndQuery + macNewLine + hostName + macNewLine + hostPort + macNewLine + macExtension + macNewLine;
        String signature = GetHash(macKey, normalized, algorithmType);
        String header = "MAC token=\"" + token + "\",timestamp=\"" + timestamp + "\",nonce=\"" + nonce + "\",signature=\"" + signature + "\"";
        return "Authorization: " + header;
    }

    //Maps field names in response with database. It uses only the fields that are passed to verify later.
    public boolean MapResponseFieldsWithDatabaseFields(List<String> responseFields, List<String> databaseFields) {
        if (responseToDatabaseMap != null) responseToDatabaseMap.clear();
        int size = responseFields.size();
        if (size != databaseFields.size()) {
            errors += "The response field size does not match with database field size. Please check your Mapping input.\n\n";
            return false;
        }
        for (int i = 0; i < size; ++i)
            responseToDatabaseMap.put(responseFields.get(i), databaseFields.get(i));
        return true;
    }


    private String GetHash(String key, String text, String hmacAlgorithm) throws Exception {
        if (hmacAlgorithm.contains("sha-256"))
            hmacAlgorithm = "HmacSHA256";
        else if (hmacAlgorithm.contains("sha-1"))
            hmacAlgorithm = "HmacSHA1";

        SecretKey hmacKey = new SecretKeySpec(key.getBytes("UTF-8"), hmacAlgorithm);
        Mac mac = Mac.getInstance(hmacKey.getAlgorithm());
        mac.init(hmacKey);

        byte[] messageBytes = text.getBytes("UTF-8");
        byte[] hashMessage = mac.doFinal(messageBytes);

        String encodedContext = Base64.encodeBase64String(hashMessage);
        return encodedContext;
    }


    public boolean setHeaders(String passedHeaders) {
        contentTypeHeaderPassed = "";
        List<String> headersStringList = Arrays.asList(passedHeaders.split("\n"));
        headersStringList.forEach(header -> {
            String temp = header;
            if (header.contains("application/xml")) contentTypeHeaderPassed = "application/xml";
            if (header.contains("application/json")) contentTypeHeaderPassed = "application/json";
            if (header.contains("application/x-www-form-urlencoded"))
                contentTypeHeaderPassed = "application/x-www-form-urlencoded";
            headersMap.put(temp.split(":")[0].trim(), temp.replace(temp.split(":")[0] + ":", "").trim());
        });
        return true;
    }

    public boolean setRequestBody(String bodyPassed) {
        body = modifiedBodyForPUT = bodyPassed;
        return true;
    }

    public String GetModifiedBody() {
        return modifiedBodyForPUT;
    }

    public int GETRequestStatusCode() {
        statusCode = executeRestRequestAndReturnStatusCode("GET");
        return statusCode;
    }

    public String GETRequestAndStoreBodyForPUT() {
        int statusCode = executeRestRequestAndReturnStatusCode("GET");
        if (statusCode != 200)
            return "GET Request Status Code is:" + statusCode;
        modifiedBodyForPUT = response.getBody().print();
        return "Success";
    }

    public int DELETERequestStatusCode() {
        return executeRestRequestAndReturnStatusCode("DELETE");
    }

    public int POSTRequestStatusCode() {
        return executeRestRequestAndReturnStatusCode("POST");
    }

    public int PUTRequestStatusCode() {
        return executeRestRequestAndReturnStatusCode("PUT");
    }

    private int executeRestRequestAndReturnStatusCode(String verbType) {
        try {
            switch (verbType) {
                case "PUT":
                    body = (modifiedBodyForPUT.length() > 0) ? modifiedBodyForPUT : body;
                    response = prepRequest().body(body).contentType(contentTypeHeaderPassed).put("");
                    break;
                case "POST":
                    response = prepRequest().body(body).contentType(contentTypeHeaderPassed).post("");
                    break;
                case "DELETE":
                    response = prepRequest().delete();
                    break;
                case "GET":
                    response = prepRequest().get();
                    break;
                default:
                    errors += "The verb type should be one of these - GET, POST, PUT, DELETE.\n\n";
                    return 0;
            }
        } catch (Exception e) {
            if (e.toString().contains("Don't know how to encode"))
                errors += "Don't know how to encode body as a byte stream. Please pass the content type header.\n\n";
            else
                errors += e.toString() + "\n\n";
            return 0;
        } finally {
            //Clean up the list for next request
            headersMap.clear();
            modifiedBodyForPUT = body = "";
        }
        return response.getStatusCode();
    }

    private RequestSpecification prepRequest() {
        //Clean up previous response
        if (response != null) response = delete();

        baseURI = endpoint.trim();
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.addHeaders(headersMap);
        return given()
                .spec(requestSpecBuilder.build())
                .when();
    }

    public String RemoveNodeInBody(String nodeNamePassed) {
        return modifyBodyForPUT(nodeNamePassed, "", true);
    }

    public String ReplaceNodeWithInBody(String nodeNamePassed, String valuePassed) {
        return modifyBodyForPUT(nodeNamePassed, valuePassed, false);
    }

    public String ReplaceNodesWithInBody(List<String> nodeNames, List<String> values) {
        int count = nodeNames.size();
        String result = "Success";
        for (int i = 0; i < count; ++i) {
            String individualResult = modifyBodyForPUT(nodeNames.get(i), values.get(i), false);
            result += individualResult.contentEquals("Success") ? "" : individualResult;
        }
        return (result.contentEquals("Success")) ? "Success" : result.replaceFirst("Success", "");
    }

    private String modifyBodyForPUT(String nodeName, String valuePassed, boolean shouldNodeBeRemoved) {
        modifiedBodyForPUT = (modifiedBodyForPUT.length() > 0) ? modifiedBodyForPUT : body;
        BufferedReader bufReader = new BufferedReader(new StringReader(modifiedBodyForPUT));
        String currentLine = null;
        String currentValue;
        int totalLinesWithTheGivenNode = 0;
        try {
            modifiedBodyForPUT = "";
            while ((currentLine = bufReader.readLine()) != null) {
                if (currentLine.trim().startsWith("\"" + nodeName + "\"")) {
                    totalLinesWithTheGivenNode++;
                    currentValue = currentLine.split(":")[1].toString().trim();
                    if (shouldNodeBeRemoved) {
                        currentLine = bufReader.readLine();
                        //Remove comma if required
                        if (currentLine != null && currentLine.trim().startsWith("}") && modifiedBodyForPUT.trim().endsWith(",")) {
                            modifiedBodyForPUT = modifiedBodyForPUT.trim().substring(0, modifiedBodyForPUT.lastIndexOf(","));
                        }
                    } else {
                        //null set to string
                        if (currentValue.startsWith("null") && !valuePassed.trim().contentEquals("null"))
                            currentLine = "\"" + nodeName + "\" : \"" + valuePassed + "\"" + (currentLine.endsWith(",") ? "," : "");
                        //String
                        if (currentValue.startsWith("\""))
                            currentLine = "\"" + nodeName + "\" : \"" + valuePassed + "\"" + (currentLine.endsWith(",") ? "," : "");
                        //number
                        if (currentValue.substring(0, 1).matches("\\d"))
                            currentLine = "\"" + nodeName + "\" : " + valuePassed + (currentLine.endsWith(",") ? "," : "");
                        //boolean & null set to null again
                        if ((currentValue.startsWith("true") || currentValue.startsWith("false") || currentValue.startsWith("null") && (valuePassed.trim().contentEquals("true") || valuePassed.trim().contentEquals("false") || valuePassed.trim().contentEquals("null"))))
                            currentLine = "\"" + nodeName + "\" : " + valuePassed + (currentLine.endsWith(",") ? "," : "");
                        //Object and array
                        if (currentValue.endsWith("[") || currentValue.endsWith("{")) {
                            errors += "Cannot replace the value as this node \"" + nodeName + "\" contains either an array or an object.\n\n";
                            return "Cannot replace the value as this node \"" + nodeName + "\" contains either an array or an object.";
                        }
                    }
                }
                modifiedBodyForPUT += currentLine + "\n";
            }
        } catch (IOException e) {
            errors += "Could not read the body.\n\n";
            return "Could not read the body.";
        }
        if (totalLinesWithTheGivenNode != 1 && !valuePassed.startsWith("[")) {
            modifiedBodyForPUT = "";
            String textToReturn = totalLinesWithTheGivenNode == 0 ? "There are no item with the field \"" + nodeName + "\". Check if you spelled the node name correctly." : "There are total " + totalLinesWithTheGivenNode + " nodes with \"" + nodeName;
            errors += textToReturn + "\n\n";
            return textToReturn;
        }
        return "Success";
    }

    public int GetStatusCode() {
        return response.getStatusCode();
    }

    public String GetNode(String nodeName) {
        try {
            if (contentTypeHeaderPassed.contains("xml")) {
                String returnValue = response.body().xmlPath().get().getNode(nodeName).value();
                return returnValue;
            }
            String nodeValue = response.body().jsonPath().getString(nodeName);
            if (nodeValue.contentEquals("[null]"))
                return null;
            return response.body().jsonPath().get(nodeName).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> GetNodeArray(String nodeName) {
        try {
            return response.body().jsonPath().get(nodeName);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> GetValuesOfNodes(List<String> nodes) {
        List<String> values = new ArrayList();
        nodes.forEach(eachNode -> values.add(GetNode(eachNode).trim()));
        return values;
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

    public String GetResponseBody() throws Exception {
        if (response == null) return null;
        return response.getBody().prettyPrint();
    }

    //Use this to compare the result of the specified fields(in responseToDatabaseMap) with the passed value
    public String ValidateResponseWith(List<String> valuesPassed) {
        Object[] nodeNamesObject = responseToDatabaseMap.keySet().toArray();
        List<String> nodeNamesString = Arrays.asList(Arrays.copyOf(nodeNamesObject, nodeNamesObject.length, String[].class));
        return ValidateNodesWith(nodeNamesString, valuesPassed);
    }

    public String ValidateNodesWith(List<String> nodeNamesPassed, List<String> valuesPassed) {
        String textToReturn = "Success";
        if (nodeNamesPassed.size() != valuesPassed.size())
            return "The mapped nodes or the node name list size is not same as value list.";
        for (int i = 0; i < valuesPassed.size(); ++i) {
            String nodeValue = GetNode(nodeNamesPassed.get(i).toString());
            if (nodeValue == null) {
                textToReturn += "Node \"" + nodeNamesPassed.get(i) + "\" not found in the response.\n";
            } else if (!valuesPassed.get(i).contentEquals(nodeValue))
                textToReturn += "In the response, value for node \"" + nodeNamesPassed.get(i).toString() + "\" is :" + nodeValue + ". Should be :" + valuesPassed.get(i) + ".\n";
        }
        return textToReturn == "Success" ? "Success" : textToReturn.replaceFirst("Success", "");
    }


    //TODO Add method to validate against a schema

    //endregion RestCall
    //region Database

    //TODO: Need to have logic to compare date from response with database
    public String ValidateResponseWithDatabase() {
        if (response == null || response.getBody().toString().length() < 1) {
            errors += "The response may not have been generated properly.\n\n";
            return "The response may not have been generated properly.";
        }
        Iterator it = responseToDatabaseMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            String databaseValue = GetValueFromResultSet(pair.getValue().toString());
            String responseValue = GetNode(pair.getKey().toString());
            if (responseValue == null && databaseValue == null)
                continue;
            if (responseValue == null) {
                errors += pair.getKey() + " value could not be obtained in response.\n\n";
                return pair.getKey() + " value could not be obtained in response.";
            }
            if (databaseValue == null) {
                errors += pair.getValue() + " value could not be obtained in database.\n\n";
                return pair.getValue() + " value could not be obtained in database.";
            }

            responseValue = responseValue.trim();
            databaseValue = databaseValue.trim();

            //null value comparison
            if (responseValue.startsWith("null")) {
                if (!databaseValue.equalsIgnoreCase("null"))
                    return "Database value for " + pair.getKey() + " is " + databaseValue + " but should have been " + responseValue;
                else continue;
            }
            //boolean value comparison
            else if (responseValue.startsWith("true")) {
                if (!databaseValue.equals("1"))
                    return "Database value for " + pair.getKey() + " is " + databaseValue + " but should have been " + responseValue;
                else continue;
            } else if (responseValue.startsWith("false")) {
                if (!databaseValue.equals("0"))
                    return "Database value for " + pair.getKey() + " is " + databaseValue + " but should have been " + responseValue;
                else continue;
            } else if (responseValue.startsWith("{") || responseValue.startsWith("["))
                //TODO have to check how to do the validations for arrays and objects.
                throw new NotImplementedException();

                //Everything else is deduced as String (Real string and number values)
            else if (!responseValue.contentEquals(databaseValue))
                return "Database value for " + pair.getKey() + " is " + databaseValue + " but should have been " + responseValue;
            it.remove(); // avoids a ConcurrentModificationException
        }
        return "Success";
    }

    public boolean RunQueryInDatabase(String query, String databaseConnectionString) throws Exception {
        dbQueryResultSet = null; //Clear result first.
        dbQueryResultSet = GetResultFirstRow(query, databaseConnectionString);
        return true;
    }

    //TODO This method assumes there will be only one result row. Modify or add new method to work with multiple rows, if needed.
    public String GetValueFromResultSet(String value) {
        if (dbQueryResultSet.size() == 0) return null;
        return dbQueryResultSet.get(value);
    }
    //endregion Database


}