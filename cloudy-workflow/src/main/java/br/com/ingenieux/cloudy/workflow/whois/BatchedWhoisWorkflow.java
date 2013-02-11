package br.com.ingenieux.cloudy.workflow.whois;

import java.util.Collection;

import br.com.ingenieux.cloudy.workflow.SimpleWorkflow;
import br.com.ingenieux.cloudy.workflow.SimpleWorkflowContext;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.model.ChildPolicy;

@Workflow
@WorkflowRegistrationOptions(description = "Executes whois on a set of hosts", defaultTaskStartToCloseTimeoutSeconds = 180, defaultChildPolicy = ChildPolicy.TERMINATE, defaultExecutionStartToCloseTimeoutSeconds = 86400)
public class BatchedWhoisWorkflow extends SimpleWorkflow<BatchedWhoisActivities, BatchedWhoisContext> {
	@Execute(name="execute", version="1")
	public void execute(BatchedWhoisContext c) {
		Promise<Collection<String>> hosts = activities.lookupHosts();
	}

}
