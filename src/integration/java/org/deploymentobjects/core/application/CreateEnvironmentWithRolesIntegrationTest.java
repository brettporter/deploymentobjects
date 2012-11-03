package org.deploymentobjects.core.application;

import static org.junit.Assert.assertEquals;

import org.deploymentobjects.core.domain.model.configuration.ConfigurationManagement;
import org.deploymentobjects.core.domain.model.configuration.NfsMount;
import org.deploymentobjects.core.domain.model.environment.EnvironmentRepository;
import org.deploymentobjects.core.domain.model.environment.Host;
import org.deploymentobjects.core.domain.model.environment.Hypervisor;
import org.deploymentobjects.core.domain.model.environment.Role;
import org.deploymentobjects.core.domain.model.environment.Host.Type;
import org.deploymentobjects.core.domain.model.execution.Dispatchable;
import org.deploymentobjects.core.domain.model.execution.ExitCode;
import org.deploymentobjects.core.domain.model.execution.Job;
import org.deploymentobjects.core.domain.shared.EventPublisher;
import org.deploymentobjects.core.domain.shared.EventStore;
import org.deploymentobjects.core.infrastructure.configuration.Puppet;
import org.deploymentobjects.core.infrastructure.execution.JschDispatch;
import org.deploymentobjects.core.infrastructure.persistence.InMemoryEventStore;
import org.deploymentobjects.core.infrastructure.persistence.zookeeper.ZookeeperEnvironmentRepository;
import org.deploymentobjects.core.infrastructure.persistence.zookeeper.ZookeeperPersistence;
import org.junit.Test;

public class CreateEnvironmentWithRolesIntegrationTest {

	@Test
	public void testScenario() throws Exception{

		EventStore eventStore = new InMemoryEventStore();
		EventPublisher publisher = new EventPublisher(eventStore);
		EnvironmentRepository repo = new ZookeeperEnvironmentRepository(
				new ZookeeperPersistence("ino:2181"), publisher);
		Dispatchable dispatch = new JschDispatch(publisher, "root");
		ConfigurationManagement configMgt = new Puppet(publisher, new Host("puppetmaster1", "peoplemerge.com",
		"192.168.10.112"), dispatch);

		CreateEnvironmentCommand command = new CreateEnvironmentCommand.Builder(
				"bigrefactor1", repo, publisher).withEventStore(eventStore).withNodes(1,
						Type.SMALL, new Hypervisor.Builder(publisher, "kowalski", new NfsMount(), dispatch).withUserName("root").build(),
						new Role("standard")).withConfigurationManagement(
						configMgt).withDispatch(
					dispatch).build();
		Job saga = command.create();
		System.out.println("saga: " + saga);
		ExitCode exit = saga.execute();
		assertEquals(ExitCode.SUCCESS, exit);
		System.out.println("event store: " + eventStore);
		System.out.println("repo: " + repo);
	}
/*
	@Test
	public void createClusterWithRoles() throws Exception {

		CreateEnvironmentCommand command = new CreateEnvironmentCommand.Builder(
				"mock1", new ZookeeperEnvironmentRepository(
						new ZookeeperPersistence("ino:2181"))).withNodes(1,
				Type.SMALL, new Hypervisor("root", "kowalski", new NfsMount()),
				new Role("standard")).withConfigurationManagement(
				new Puppet(new Host("puppetmaster1", "peoplemerge.com",
						"192.168.10.137"))).withDispatch(
				new JschDispatch("root")).build();
		ExitCode exit = command.execute();
		assertEquals(ExitCode.SUCCESS, exit);

	}
	*/
}
