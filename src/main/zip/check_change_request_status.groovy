import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.snow.ServiceNowHelper;
import com.serena.air.plugin.snow.FailMode;

final def apTool = new AirPluginTool(args[0], args[1]);
final def props = apTool.getStepProperties();
final def helper = new ServiceNowHelper(apTool);
final def apiVersion = helper.getAPIVersion();

def stateName = props['stateName'];
def failMode = FailMode.valueOf(props['failMode']);
def changeIds = props['changeIds'].split(',') as List;
def stateCount = 0;
for (def changeId : changeIds.sort()) {
    if (helper.changeRequestExists(changeId)) {
        def actualState = helper.changeRequestState(changeId);
        if (actualState == stateName) {
            println "Found \"${changeId}\" with the correct state: ${stateName}.";
            stateCount++;
        } else {
            if (failMode == FailMode.FAIL_FAST) {
                helper.exitFailure("Change Request \"${changeId}\" has an different state: ${actualState}.");
            } else {
                println "Change Request \"${changeId}\" has an different state: ${actualState}.";
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
def totalNum = changeIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!changeIds) {
        helper.exitFailure("No Change Requests found to check.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!changeIds || (stateCount != totalNum)) {
        helper.exitFailure("Only found correct state on ${stateCount} out of ${totalNum} Change Requests.");
    }
}
println "Found the correct state on ${stateCount} out of ${totalNum} Change Requests.";

System.exit(0);