/*
 * Copyright (C) 2019, TU Dortmund, Malte Mues (@mmuesly)
 * All rights reserved.
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.jdart.termination;

/**
 * On default, JDart is going to explore all possible branches as this is the true power of Symbolic Execution
 * without stopping on first error. Nevertheless, sometimes it is desirable to stop he exploration
 * on the first observed error for saving resources.
 */
public class TerminateOnFirstError extends TerminationStrategy {

	boolean errorObserved = false;
	int counter = 0;

	public void setErrorPathObserved(){
		errorObserved = true;
		++counter;
	}

	@Override
	public boolean isDone() {
		return errorObserved;
	}

	@Override
	public String getReason() {
		if(errorObserved){
			return "The exploration terminated after " + counter + " observed Errors";
		}else{
			return "The exploration has run to completion without any Error Paths.";
		}
	}
}
