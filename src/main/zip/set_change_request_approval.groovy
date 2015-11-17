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

def approvalName = props['approvalName'];
def failMode = FailMode.valueOf(props['failMode']);
def changeIds = props['changeIds'].split(',') as List;
def approvalCount = 0;
for (def changeId : changeIds.sort()) {
    String sysId = helper.changeRequestId(changeId)
    if (sysId != null) {
        HttpPut put = new HttpPut(helper.getServerURL() + "api/now/" + apiVersion +
                "/table/change_request/" + sysId +
                "?sysparm_fields=approval" +
                "&sysparm_display_value=true");
        JSONObject changeRequest = new JSONObject();
        changeRequest.put("approval", approvalName);
        HttpResponse response = helper.executeHttpRequest(put, 200, changeRequest);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String json = reader.readLine();
        JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
        JSONObject result = jsonResponse.getJSONObject("result");
        String approval = helper.getStringFromJSON(result, "approval");
        println "Set approval state of Change Request \"${changeId}\" to: ${approval}.";
        approvalCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: Change Request \"${changeId}\" not found.");
        } else {
            println "Could not find Change Request \"${changeId}\".";
        }
    }
}
def totalNum = changeIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!changeIds) {
        helper.exitFailure("No Change Requests found to set.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!changeIds || (approvalCount != totalNum)) {
        helper.exitFailure("Only set approval state on ${approvalCount} out of ${totalNum} Change Requests.");
    }
}
println "Set the approval state on ${approvalCount} out of ${totalNum} Change Requests.";

System.exit(0);