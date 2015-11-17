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

def stateName = props['stateName'];
def failMode = FailMode.valueOf(props['failMode']);
def changeIds = props['changeIds'].split(',') as List;
def totalCount = 0;
def stateCount = 0;
for (def changeId : changeIds.sort()) {
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
            totalCount += numResults;
            for (int i = 0; i < numResults; i++) {
                def crObject = results.getJSONObject(i);
                def number = helper.getStringFromJSON(crObject, "number");
                def sysId = helper.getStringFromJSON(crObject, "sys_id");
                def sDesc = helper.getStringFromJSON(crObject, "short_description");
                def state = helper.getStringFromJSON(crObject, "state");
                if (state == stateName) {
                    println "Change Task \"${number} - ${sDesc}\" has correct state: ${state}";
                    stateCount++;
                } else {
                    if (failMode == FailMode.FAIL_FAST) {
                        helper.exitFailure("Change Task \"${number} - ${sDesc}\" has an different state: ${state}.");
                    } else {
                        println "Change Task \"${number} - ${sDesc}\" has an different state: ${state}.";
                    }
                }
            }
        }
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: Change Request \"${changeId}\" not found.");
        } else {
            println "Could not find Change Request \"${changeId}\".";
        }
    }
}
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!changeIds) {
        helper.exitFailure("No Change Requests found to check.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!changeIds || (stateCount != totalCount)) {
        helper.exitFailure("Only found correct state on ${stateCount} out of ${totalCount} Change Tasks.");
    }
}
println "Found the correct state on ${stateCount} out of ${totalCount} Change Tasks.";

System.exit(0);