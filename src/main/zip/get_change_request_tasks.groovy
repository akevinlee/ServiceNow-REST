import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.snow.ServiceNowHelper;
import com.serena.air.plugin.snow.FailMode
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener;

final def apTool = new AirPluginTool(args[0], args[1]);
final def props = apTool.getStepProperties();
final def helper = new ServiceNowHelper(apTool);
final def apiVersion = helper.getAPIVersion();

def failMode = FailMode.valueOf(props['failMode']);
def changeId = props['changeId'];
def taskCount = 0;
String changeTaskIds = "";

if (helper.changeRequestExists(changeId)) {
    HttpGet get = new HttpGet(helper.getServerURL() + "api/now/" + apiVersion +
            "/table/change_task?sysparm_query=change_request.number=" + changeId +
            "&sysparm_fields=number,sys_id,short_description,state" +
            "&sysparm_display_value=true");
    HttpResponse response = helper.executeHttpRequest(get, 200, null);
    if (response.getStatusLine().getStatusCode() == 404) {
        println "No Change Task's found for Change Request \"${changeId}\".";
    } else {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String json = reader.readLine();
        JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
        JSONArray results = jsonResponse.getJSONArray("result");
        def numResults = results.length();
        println "Found ${numResults} Change Task's for Change Request \"${changeId}\".";
        taskCount += numResults;
        for (int i = 0; i < numResults; i++) {
            def crObject = results.getJSONObject(i);
            def number = helper.getStringFromJSON(crObject, "number");
            def sysId = helper.getStringFromJSON(crObject, "sys_id");
            def sDesc = helper.getStringFromJSON(crObject, "short_description");
            def state = helper.getStringFromJSON(crObject, "state");
            if (props['debug']) println ">>> Found Change Task \"${number}\" in state: ${state}";
            changeTaskIds += "${number},";
        }
    }
} else {
    if (failMode == FailMode.FAIL_FAST) {
        helper.exitFailure("Error: Change Request \"${changeId}\" not found.");
    } else {
        println "Could not find Change Request \"${changeId}\".";
    }
}

if (changeTaskIds.endsWith(",")) {
    changeTaskIds = changeTaskIds.subSequence(0, changeTaskIds.length()-1)
}
if (props['debug']) println ">>> Settings changeTaskIds property to \"${changeTaskIds}\".";
apTool.setOutputProperty("changeTaskIds", changeTaskIds);
apTool.setOutputProperty("changeTaskIdCount", taskCount);
apTool.setOutputProperties();

System.exit(0);