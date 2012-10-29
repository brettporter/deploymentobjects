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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.deploymentobjects.core.domain.model.configuration.ConfigurationManagement;
import org.deploymentobjects.core.domain.model.configuration.NamingService;
import org.deploymentobjects.core.domain.model.configuration.NfsMount;
import org.deploymentobjects.core.domain.model.configuration.NoConfigurationManagement;
import org.deploymentobjects.core.domain.model.environment.Environment;
import org.deploymentobjects.core.domain.model.environment.EnvironmentRepository;
import org.deploymentobjects.core.domain.model.environment.Host;
import org.deploymentobjects.core.domain.model.environment.HostPool;
import org.deploymentobjects.core.domain.model.environment.Hypervisor;
import org.deploymentobjects.core.domain.model.environment.Role;
import org.deploymentobjects.core.domain.model.environment.provisioning.KickstartTemplateService;
import org.deploymentobjects.core.domain.model.execution.BlockingEventStep;
import org.deploymentobjects.core.domain.model.execution.ConcurrentSteps;
import org.deploymentobjects.core.domain.model.execution.Dispatchable;
import org.deploymentobjects.core.domain.model.execution.DispatchableStep;
import org.deploymentobjects.core.domain.model.execution.ExitCode;
import org.deploymentobjects.core.domain.model.execution.Job;
import org.deploymentobjects.core.domain.model.execution.PersistStep;
import org.deploymentobjects.core.domain.model.execution.SequentialSteps;
import org.deploymentobjects.core.domain.shared.EventPublisher;
import org.deploymentobjects.core.domain.shared.EventStore;
import org.deploymentobjects.core.infrastructure.configuration.TemplateHostsFile;
import org.deploymentobjects.core.infrastructure.execution.JschDispatch;
import org.deploymentobjects.core.infrastructure.execution.Ssh;
import org.deploymentobjects.core.infrastructure.persistence.InMemoryEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateEnvironmentCommand implements CreatesJob {

	private List<Host> nodes = new ArrayList<Host>();
	private String environmentName;
	// private Persistence persistence;
	private EnvironmentRepository repo;
	private Dispatchable dispatchable;
	private KickstartTemplateService kickstartServer;
	private NamingService namingService;
	private Logger logger = LoggerFactory
			.getLogger(CreateEnvironmentCommand.class);
	private ConfigurationManagement configurationManagement;
	private EventStore eventStore;
	private EventPublisher publisher;
	private Environment environment;

	private CreateEnvironmentCommand() {
	}

	public static class Builder {

		// "Create a new environment called development using 1 small nodes from dom0."
		CreateEnvironmentCommand command = new CreateEnvironmentCommand();

		public Builder(String environmentName, EnvironmentRepository repo, EventPublisher publisher) {
			command.environmentName = environmentName;
			command.repo = repo;
			command.publisher = publisher;
		}

		public Builder withNodes(int quantity, Host.Type type, HostPool pool,
				Role... roles) {
			for (int i = 1; i <= quantity; i++) {
				String roleName = "";
				for (Role role : roles) {
					roleName += role.getName();
				}
				Host node = new Host(command.environmentName + roleName + i,
						"peoplemerge.com", type, pool, roles);
				command.nodes.add(node);
			}
			return this;
		}

		public Builder withDispatch(Dispatchable d) {
			command.dispatchable = d;
			return this;
		}

		public Builder withKickstartServer(
				KickstartTemplateService kickstartServer) {
			command.kickstartServer = kickstartServer;
			return this;
		}

		public Builder withNamingService(NamingService namingService) {
			command.namingService = namingService;
			return this;
		}

		public Builder withLogger(Logger logger) {
			command.logger = logger;
			return this;
		}

		public Builder withConfigurationManagement(ConfigurationManagement cm) {
			command.configurationManagement = cm;
			return this;
		}

		public Builder withEventStore(EventStore eventStore) {
			command.eventStore = eventStore;
			return this;
		}

		public CreateEnvironmentCommand build() {
			if (command.dispatchable == null) {
				command.dispatchable = new JschDispatch(System
						.getProperty("user.name"));
			}
			if (command.configurationManagement == null) {
				command.configurationManagement = new NoConfigurationManagement();
			}
			if (command.environment == null) {
				command.environment = new Environment(command.environmentName);
				command.environment.getHosts().addAll(command.nodes);
			}
			if (command.kickstartServer == null) {
				command.kickstartServer = KickstartTemplateService.factory(
						command.publisher, command.environment,
						"/mnt/media/software/kickstart", new NfsMount(),
						command.configurationManagement);
			}
			if (command.namingService == null) {
				command.namingService = new TemplateHostsFile();
			}
			if (command.eventStore == null) {
				command.eventStore = new InMemoryEventStore();
			}
			return command;
		}

	}

	private long startTime;

	public Job create() {
		

		Ssh ssh = Ssh.factory(publisher);

		
		SequentialSteps sequence = new SequentialSteps(publisher);
		Job saga = new Job(publisher, sequence);

		for (Host node : nodes) {
			sequence.add(node.getSource().createStep(node.getType(),
					node.getHostname()));
		}

		
		ConcurrentSteps concurrent = new ConcurrentSteps(publisher);
		for(Host host : environment.getHosts()){
			BlockingEventStep sshToVms = ssh.buildStepFor(environment, host, "echo hello world");
			concurrent.add(sshToVms);
		}
		sequence.add(concurrent);
		
		PersistStep persistStep = new PersistStep(repo, publisher,environment);
		sequence.add(persistStep);
		

		return saga;
	}


	public ExitCode execute() {
		startTime = System.currentTimeMillis();

		Environment environment = new Environment(environmentName);
		for (Host node : nodes) {
			// TODO extract these timing variables to the grammar
			logger.info("Polling for " + node + " to stop");

			node.getSource().pollForDomainToStop(node.getHostname(), 500,
					600000);
			logger.info("Restarting " + node);
			node.getSource().startHost(node.getHostname());

			logger.info("Signing puppet cert for " + node);

			DispatchableStep step = configurationManagement
					.postCompleteStep(node);
			ExitCode exitcode;
			try {
				exitcode = dispatchable.dispatch(step);
			} catch (Exception e) {
				logger.error("Exception dispatching " + e.toString());
				return ExitCode.FAILURE;
			}
			if (exitcode != ExitCode.SUCCESS) {
				logger.error("Dispatch execution failed: " + step.getOutput());
				return ExitCode.FAILURE;
			}
			environment.addHost(node);
		}

		logger.info("Waiting for nodes to write to Zookeeper");
		try {
			repo.blockUntilProvisioned(environment);
		} catch (InterruptedException e) {
			logger.error(e.toString());
			return ExitCode.FAILURE;
		}

		try {
			logger.info("Saving " + environmentName + " to repo " + repo);
			repo.save(environment);
		} catch (Exception e) {
			logger.error(e.toString());
			return ExitCode.FAILURE;
		}

		logger.info("Updating " + namingService);

		namingService.update(repo);

		try {
			logger.info("Updating " + configurationManagement);
			DispatchableStep step = configurationManagement
					.newEnvironment(repo);
			ExitCode exitcode = dispatchable.dispatch(step);
			if (exitcode != ExitCode.SUCCESS) {
				logger.error("Dispatch execution failed: " + step.getOutput());
				return ExitCode.FAILURE;
			}

		} catch (Exception e) {
			logger.error(e.toString());
			return ExitCode.FAILURE;
		}

		try {
			for (Host node : nodes) {
				logger.info("Using " + configurationManagement
						+ " to complete node provisioning on " + node);
				DispatchableStep step = configurationManagement
						.nodeProvisioned(node);
				ExitCode exitcode = dispatchable.dispatch(step);
				if (exitcode != ExitCode.SUCCESS) {
					logger.error("Dispatch execution failed: "
							+ step.getOutput());
					return ExitCode.FAILURE;
				}
			}

		} catch (Exception e) {
			logger.error(e.toString());
			return ExitCode.FAILURE;
		}

		long duration = (System.currentTimeMillis() - startTime) / 1000;
		logger.info("Operation completed in " + duration + "s");
		return ExitCode.SUCCESS;

	}

	public String toString() {
		return new ReflectionToStringBuilder(this).toString();
	}

}
