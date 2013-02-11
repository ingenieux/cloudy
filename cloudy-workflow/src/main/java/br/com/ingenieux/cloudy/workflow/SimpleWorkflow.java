package br.com.ingenieux.cloudy.workflow;


/**
 * A Base class for SWF Process Definitions
 * 
 * @author aldrin
 * 
 */
public class SimpleWorkflow<A extends SimpleWorkflowActivities, C extends SimpleWorkflowContext> {
	protected A activities;
	
	protected C context;
}
