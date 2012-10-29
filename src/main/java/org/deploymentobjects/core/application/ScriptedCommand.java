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
package org.deploymentobjects.core.application;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.deploymentobjects.core.domain.model.execution.Executable;
import org.deploymentobjects.core.domain.model.execution.ExitCode;
import org.deploymentobjects.core.domain.model.execution.Script;

public class ScriptedCommand extends Executable {

	@Override
	public ExitCode execute() {
		// TODO
		return ExitCode.FAILURE;
	}
	
	private Script body;

	public ScriptedCommand(String body){
		this.body = new Script(body);
	}

	public Script getBody() {
		return body;
	}
	
	public String toString(){
		return body.toString();
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
		ScriptedCommand rhs = (ScriptedCommand) obj;
		// TODO invesigate why it fails when .appendSuper(super.equals(obj))
		return new EqualsBuilder().append(body.toString(), rhs.body.toString()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(74543, 9984405).append(body.toString()).toHashCode();
	}

}
