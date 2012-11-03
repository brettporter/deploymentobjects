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

import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.deploymentobjects.core.DeploymentObjectsLexer;
import org.deploymentobjects.core.DeploymentObjectsParser;


public class Program {

	private List<CreatesJob> steps = new LinkedList<CreatesJob>();

	public void addStep(CreatesJob executable){
		steps.add(executable);
	}

	public List<CreatesJob> getSteps() {
		return steps;
	}

	public String display() {
		return toString();
	}

	public String toString() {
		
		return steps.toString();
	}

	public void execute() {
		for (CreatesJob step : steps) {
			// Loop through nodes here too?
			// Really need to use the runner!
			System.out.println("Program executing step: " + step.toString());
			Job job = step.create();
			job.execute();
		}
	}

	public static Program factory(String sentence) throws RecognitionException {
		CharStream stringStream = new ANTLRStringStream(sentence);
		DeploymentObjectsLexer lexer = new DeploymentObjectsLexer(stringStream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		DeploymentObjectsParser parser = new DeploymentObjectsParser(tokenStream);
		Program result = parser.program().program;
		return result;
	}

}
