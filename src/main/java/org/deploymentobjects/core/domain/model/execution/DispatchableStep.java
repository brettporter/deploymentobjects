/************************************************************************
 ** 
 ** Copyright (C) 2011 Dave Thomas, PeopleMerge.
 ** All rights reserved.
 ** Contact: opensource@peoplemerge.com.
 **
 ** This file is part of the NGDS language.
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **    http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 **  
 ** Other Uses
 ** Alternatively, this file may be used in accordance with the terms and
 ** conditions contained in a signed written agreement between you and the 
 ** copyright owner.
 ************************************************************************/
package org.deploymentobjects.core.domain.model.execution;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.deploymentobjects.core.domain.model.environment.Host;
import org.deploymentobjects.core.domain.model.execution.DispatchEvent.Completed;
import org.deploymentobjects.core.domain.model.execution.DispatchEvent.Requested;
import org.deploymentobjects.core.domain.shared.EventStore;



public class DispatchableStep extends Executable {

	protected Dispatchable dispatchable;
	protected AcceptsCommands target;
	protected Script command;
	protected EventStore eventStore;

	public DispatchableStep(Script command, AcceptsCommands target, Dispatchable dispatchable, EventStore eventStore) {
		this.dispatchable = dispatchable;
		this.command = command;
		this.target = target;
		this.eventStore = eventStore;
	}


	@Override
	public ExitCode execute() {
		try {
			// TODO post to handler
			Requested requested = new Requested(new Date(), dispatchable, command, target);
			Completed completed = dispatchable.dispatch(requested);
			if(completed.isSuccessful()){
				return ExitCode.SUCCESS;
			} else {
				return ExitCode.FAILURE;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ExitCode.FAILURE;
		}
	}

	public List<Host> getHosts() {
		return target.getHosts();
	}

	public String toString() {
		if (output == null) {
			return "Running: " + command + " on " + target;
		} else {
			return "Ran: " + command + " on " + target + " result: " + output;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		DispatchableStep rhs = (DispatchableStep) obj;
		// TODO invesigate why it fails when .appendSuper(super.equals(obj))
		EqualsBuilder builder = new EqualsBuilder()
				.append(command, rhs.command).append(target, rhs.target);
		if (!(output == null && rhs.output == null)) {
			builder.append(output, rhs.output);
		}		
		return builder.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(1245737, 534515).append(command)
				.append(target).append(output).toHashCode();
	}

}
