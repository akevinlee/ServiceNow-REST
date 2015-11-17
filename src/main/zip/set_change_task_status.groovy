import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.snow.ServiceNowHelper;
import com.serena.air.plugin.snow.FailMode
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPut
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener;

final def apTool = new AirPluginTool(args[0], args[1]);
final def props = apTool.getStepProperties();
final def helper = new ServiceNowHelper(apTool);
final def apiVersion = helper.getAPIVersion();

def stateId = props['stateId'];
def failMode = FailMode.valueOf(props['failMode']);
def changeIds = props['changeTaskIds'].split(',') as List;
def stateCount = 0;
for (def changeId : changeIds.sort()) {
    String sysId = helper.changeTaskId(changeId)
    if (sysId != null) {
        HttpPut put = new HttpPut(helper.getServerURL() + "api/now/" + apiVersion +
                "/table/change_task/" + sysId +
                "?sysparm_fields=state" +
                "&sysparm_display_value=true");
        JSONObject changeRequest = new JSONObject();
        changeRequest.put("state", stateId);
        HttpResponse response = helper.executeHttpRequest(put, 200, changeRequest);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String json = reader.readLine();
        JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
        JSONObject result = jsonResponse.getJSONObject("result");
        String state = helper.getStringFromJSON(result, "state");
        println "Set state of Change Task \"${changeId}\" to: ${state}.";
        stateCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: Change Task \"${changeId}\" not found.");
        } else {
            println "Could not find Change Task \"${changeId}\".";
        }
    }
}
def totalNum = changeIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!changeIds) {
        helper.exitFailure("No Change Tasks found to check.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!changeIds || (stateCount != totalNum)) {
        helper.exitFailure("Only set state on ${stateCount} out of ${totalNum} Change Tasks.");
    }
}
println "Set the state on ${stateCount} out of ${totalNum} Change Tasks.";

System.exit(0);