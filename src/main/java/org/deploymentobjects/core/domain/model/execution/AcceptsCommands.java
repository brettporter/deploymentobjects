package org.deploymentobjects.core.domain.model.execution;

import java.util.List;

import org.deploymentobjects.core.domain.model.environment.Host;

public interface AcceptsCommands {

	//TODO refactor these sets of interfaces
	//public Job accept(Executable command);
	
	public List<Host> getHosts();
	
}
