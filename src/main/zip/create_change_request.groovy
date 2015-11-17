import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.snow.ServiceNowHelper;
import com.serena.air.plugin.snow.FailMode
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.json.JSONObject
import org.json.JSONTokener;

final def apTool = new AirPluginTool(args[0], args[1]);
final def props = apTool.getStepProperties();
final def helper = new ServiceNowHelper(apTool);
final def apiVersion = helper.getAPIVersion();

def failMode = FailMode.valueOf(props['failMode']);

HttpPost post = new HttpPost(helper.getServerURL() + "api/now/" + apiVersion +
        "/table/change_request" +
        "?sysparm_fields=number,sys_id" +
        "&sysparm_display_value=true");

JSONObject changeRequest = new JSONObject();
changeRequest.put("short_description", props['summary']);
changeRequest.put("description", props['description']);
if (props['crType'])        changeRequest.put("type", props['crType']);
if (props['crCategory'])    changeRequest.put("category", props['crCategory']);
if (props['crPriority'])    changeRequest.put("priority", props['crPriority']);
if (props['crRisk'])        changeRequest.put("risk", props['crRisk']);
if (props['crImpact'])      changeRequest.put("impact", props['crImpact']);
if (props['configItem'])    changeRequest.put("cmdb_ci", props['configItem']);
// additional fields
if (props['additionalFields']) {
    props['additionalFields'].split('\n').collect {
        def (fldName, fldVal) = it.tokenize('=');
        changeRequest.put(fldName, fldVal);
    }
}

HttpResponse response = helper.executeHttpRequest(post, 201, changeRequest);
BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
String json = reader.readLine();
JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
JSONObject result = jsonResponse.getJSONObject("result");
String changeId = helper.getStringFromJSON(result, "number");
String sysId = helper.getStringFromJSON(result, "sys_id");

println "Succesfully created Change Request \"${changeId}\".";
println "See " + helper.getServerURL() + "nav_to.do?uri=change_reqeust.do?sys_id=${sysId} for more information."

apTool.setOutputProperty("changeId", changeId);
apTool.setOutputProperty("sysId", sysId);
apTool.setOutputProperties();


System.exit(0);