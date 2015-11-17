package com.serena.air.plugin.snow;

import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.snow.FailMode;

import org.apache.http.client.HttpClient
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpException;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;

public class ServiceNowHelper {

    private AirPluginTool pluginTool;
    private String username;
    private String password;
    private String serverURL;
    private String authToken;
    private String failMode;
    private String apiVersion;
    private def props;

    /**
     * Constructs an ServiceNow Helper and creates an authentication header for REST calls
     * @params pluginTool The AirPluginTool containing all step properties
     */
    public ServiceNowHelper(AirPluginTool pluginTool) {
        this.pluginTool = pluginTool;
        this.props = this.pluginTool.getStepProperties();
        if ((props['username'] == null) || (props['serverUrl'] == null))
            exitFailure("A username, password and server URL have not been provided.");
        this.username = props['username'];
        this.password = props['password'];
        if (props['serverUrl'].endsWith("/")) {
            this.serverURL = props['serverUrl'];
        } else {
            this.serverURL = props['serverUrl'] + "/";
        }
        String creds = this.username + ':' + this.password;
        this.authToken = "Basic " + creds.bytes.encodeBase64().toString();
        if (props['failMode'])
            this.failMode = FailMode.valueOf(props['failMode']);
        if (props['apiVersion'] && props['apiVersion'] != "latest") {
            this.apiVersion = props['apiVersion'];
        } else {
            this.apiVersion = "v1";
        }
    }

    //
    // public methods
    //

    public getServerURL() { return this.serverURL; }
    public getAPIVersion() { return this.apiVersion; }

    /**
     * Check if a change request exists
     * @param changeNumber The change by number to check for
     * @return true if the change exists, else false
     */
    public boolean changeRequestExists(String changeNumber) {
        if (props['debug']) println ">>> Checking if Change Request \"${changeNumber}\" exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "api/now/${this.apiVersion}/table/change_request?sysparm_query=number=" + changeNumber +
                    "&sysparm_fields=sys_id" +
                    "&sysparm_display_value=true");
            HttpResponse response = executeHttpRequest(get, 200, null);
            if (response.getStatusLine().getStatusCode() == 404) {
                println "The change request \"${changeNumber}\" does not exist, or is not visible to the user.";
                return false;
            }
            if (props['debug']) println ">>> Found Change Request \"${changeNumber}\".";
            return true;
        } catch (HttpResponseException ex) {
            println "The Change Request \"${changeNumber}\" does not exist, or is not visible to the user.";
            return false;
        }
    }

    /**
     * Check if a change request exists and if so return its sys_id
     * @param changeNumber The change by number to check for
     * @return the sys_id if the change exists, else null
     */
    public String changeRequestId(String changeNumber) {
        String sysId = null;
        if (props['debug']) println ">>> Checking if Change Request \"${changeNumber}\" exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "api/now/${this.apiVersion}/table/change_request?sysparm_query=number=" + changeNumber +
                    "&sysparm_fields=sys_id");
            HttpResponse response = executeHttpRequest(get, 200, null);
            if (response.getStatusLine().getStatusCode() == 404) {
                println "The Change Request \"${changeNumber}\" does not exist, or is not visible to the user.";
                return sysId;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine()
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            JSONArray results = jsonResponse.getJSONArray("result");
            sysId = getStringFromJSON(results.getJSONObject(0), "sys_id");
            if (props['debug']) println ">>> Found Change Request \"${changeNumber}\" with sys_id: ${sysId}.";
            return sysId;
        } catch (HttpResponseException ex) {
            println "The Change Request \"${changeNumber}\" does not exist, or is not visible to the user.";
            return null;
        }
    }

    /**
     * Check if a change task exists and if so return its sys_id
     * @param changeNumber The change by number to check for
     * @return the sys_id if the change exists, else null
     */
    public String changeTaskId(String changeNumber) {
        String sysId = null;
        if (props['debug']) println ">>> Checking if Change Task \"${changeNumber}\" exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "api/now/${this.apiVersion}/table/change_task?sysparm_query=number=" + changeNumber +
                    "&sysparm_fields=sys_id");
            HttpResponse response = executeHttpRequest(get, 200, null);
            if (response.getStatusLine().getStatusCode() == 404) {
                println "The Change Task \"${changeNumber}\" does not exist, or is not visible to the user.";
                return sysId;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine()
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            JSONArray results = jsonResponse.getJSONArray("result");
            sysId = getStringFromJSON(results.getJSONObject(0), "sys_id");
            if (props['debug']) println ">>> Found Change Task \"${changeNumber}\" with sys_id: ${sysId}.";
            return sysId;
        } catch (HttpResponseException ex) {
            println "The Change Task \"${changeNumber}\" does not exist, or is not visible to the user.";
            return null;
        }
    }

    /**
     * Get the state of a change request
     * @param changeNumber The change by number to check for
     * @return the state of the change if it exists, else -1
     */
    public String changeRequestState(String changeNumber) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "api/now/${this.apiVersion}/table/change_request?sysparm_query=number=" + changeNumber +
                    "&sysparm_fields=number,sys_id,state,approval" +
                    "&sysparm_display_value=true");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine()
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            JSONArray results = jsonResponse.getJSONArray("result");
            def state = getStringFromJSON(results.getJSONObject(0), "state");
            if (props['debug']) println ">>> Found Change Request \"${changeNumber}\" with state: ${state}.";
            return state;
        } catch (HttpResponseException ex) {
            exitFailure("The Change Request \"${changeNumber}\" does not exist, or is not visible to the user.");
            return -1;
        }
    }

    /**
     * Get the state of a change request task
     * @param changeTaskNumber The change by number to check for
     * @return the state of the change request task if it exists, else -1
     */
    public String changeRequestTaskState(String changeTaskNumber) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "api/now/${this.apiVersion}/table/change_task?sysparm_query=number=" + changeTaskNumber +
                    "&sysparm_fields=number,sys_id,state,approval" +
                    "&sysparm_display_value=true");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            JSONArray results = jsonResponse.getJSONArray("result");
            def state = getStringFromJSON(results.getJSONObject(0), "state");
            if (props['debug']) println ">>> Found Change Request task \"${changeTaskNumber}\" with state: ${state}.";
            return state;
        } catch (HttpResponseException ex) {
            exitFailure("The Change Request task \"${changeTaskNumber}\" does not exist, or is not visible to the user.");
            return -1;
        }
    }

    /**
     * Get the approval state of a change request
     * @param changeNumber The change by number to check for
     * @return the approval state of the change if it exists, else -1
     */
    public String changeRequestApprovalState(String changeNumber) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "api/now/${this.apiVersion}/table/change_request?sysparm_query=number=" + changeNumber +
                    "&sysparm_fields=approval" +
                    "&sysparm_display_value=true");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            JSONArray results = jsonResponse.getJSONArray("result");
            def approval = getStringFromJSON(results.getJSONObject(0), "approval");
            if (props['debug']) println ">>> Found Change Request \"${changeNumber}\" with approval state: ${approval}.";
            return approval;
        } catch (HttpResponseException ex) {
            exitFailure("The Change Request \"${changeNumber}\" does not exist, or is not visible to the user.");
            return -1;
        }
    }

    /**
     * Gets a string from a JSON object
     * @param obj the JSON object
     * @param name the name of the name/value string to match
     * @return the string if it exists and is not null, else an empty string
     */
    public String getStringFromJSON(JSONObject obj, String name) {
        if (obj.has(name)) {
            if (!obj.isNull(name)) {
                return obj.getString(name)
            }
        }
        return "";
    }


    /**
     * Executes the given HTTP request and checks for a correct response status
     * @param request The HttpRequest to execute
     * @param expectedStatus The response status that indicates a successful request
     * @param body The JSONObject containing the request body
     * @return A JSONObject containing the response to the HTTP request executed
     */
    private HttpResponse executeHttpRequest(Object request, int expectedStatus, JSONObject body) {
        // Make sure the required parameters are there
        if ((request == null) || (expectedStatus == null)) exitFailure("An error occurred executing the request.");

        if (props['debug']) {
            println ">>> Sending request: ${request}"
            if (body != null) println "\n>>> Body contents: ${body}";
        }

        HttpClient client = new DefaultHttpClient();
        request.setHeader("Authorization", this.authToken);
        if (body) {
            StringEntity input = new StringEntity(body.toString());
            input.setContentType("application/json");
            request.setEntity(input);
        }

        HttpResponse response;
        try {
            response = client.execute(request);
        } catch (HttpException e) {
            exitFailure("There was an error executing the request.");
        }

        int responseCode = response.getStatusLine().getStatusCode();
        if ((responseCode != expectedStatus) && responseCode != 404) {
            httpFailure(response);
        }

        if (props['debug']) {
            println ">>> Received the response: " + response.getStatusLine();
        }
        return response;
    }

    /**
     * Write an error message to console and exit on a fail status.
     * @param message The error message to write to the console.
     */
    private void exitFailure(String message) {
        println "${message}";
        System.exit(1);
    }

    /**
     * Write a HTTP error message to console and exit on a fail status.
     * @param message The error message to write to the console.
     */
    private void httpFailure(HttpResponse response) {
        println ">>> Request failed: " + response.getStatusLine();
        String responseString = new BasicResponseHandler().handleResponse(response);
        println "${responseString}";
        System.exit(1);
    }

    //
    // private methods
    //

}