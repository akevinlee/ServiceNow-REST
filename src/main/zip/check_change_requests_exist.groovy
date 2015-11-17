import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.snow.ServiceNowHelper;
import com.serena.air.plugin.snow.FailMode;

final def apTool = new AirPluginTool(args[0], args[1]);
final def props = apTool.getStepProperties();
final def helper = new ServiceNowHelper(apTool);
final def apiVersion = helper.getAPIVersion();

def failMode = FailMode.valueOf(props['failMode']);
def changeIds = props['changeIds'].split(',') as List;
def checkCount = 0;
for (def changeId : changeIds.sort()) {
    if (helper.changeRequestExists(changeId)) {
        println "Found Change Request \"${changeId}\".";
        checkCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Change Request \"${changeId}\" not found.");
        } else {
            println "Could not find Change Request \"${changeId}\".";
        }
    }
}
def totalNum = changeIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!changeIds) {
        helper.exitFailure("No Change Requests found.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!changeIds || (checkCount != totalNum)) {
        helper.exitFailure("Only found ${checkCount} out of ${totalNum} Change Requests.");
    }
}
println "Found ${checkCount} out of ${totalNum} Change Requests.";

System.exit(0);