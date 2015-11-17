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
        "/table/incident" +
        "?sysparm_fields=number,sys_id" +
        "&sysparm_display_value=true");

JSONObject incident = new JSONObject();
incident.put("short_description", props['summary']);
incident.put("description", props['description']);
if (props['incCategory'])    incident.put("category", props['incCategory']);
if (props['incSubCategory']) incident.put("sub_category", props['incSubCategory']);
if (props['incImpact'])      incident.put("impact", props['incImpact']);
if (props['incUrgency'])      incident.put("urgency", props['incUrgency']);
if (props['configItem'])     incident.put("cmdb_ci", props['configItem']);
// additional fields
if (props['additionalFields']) {
    props['additionalFields'].split('\n').collect {
        def (fldName, fldVal) = it.tokenize('=');
        incident.put(fldName, fldVal);
    }
}

HttpResponse response = helper.executeHttpRequest(post, 201, incident);
BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
String json = reader.readLine();
JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
JSONObject result = jsonResponse.getJSONObject("result");
String incidentId = helper.getStringFromJSON(result, "number");
String sysId = helper.getStringFromJSON(result, "sys_id");

println "Succesfully created Incident \"${incidentId}\".";
println "See " + helper.getServerURL() + "nav_to.do?uri=incident.do?sys_id=${sysId} for more information."

apTool.setOutputProperty("incidentId", incidentId);
apTool.setOutputProperty("sysId", sysId);
apTool.setOutputProperties();


System.exit(0);