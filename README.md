![Trident](https://raw.githubusercontent.com/LendingClub/mercator/master/.assets/noun_773008_sm.png) 

# Mercator

[![Circle CI](https://circleci.com/gh/LendingClub/mercator.svg?style=svg)](https://circleci.com/gh/LendingClub/mercator")
[![Download](https://api.bintray.com/packages/lendingclub/OSS/mercator/images/download.svg)](https://bintray.com/lendingclub/OSS/mercator/_latestVersion)


Mercator creates graph-model projections of physical, virtual and cloud infrastructure.

Mercator uses [Neo4j](https://neo4j.com/), a particularly awesome open-source graph database.  

It is intended to be used as a library inside of other services and tools.

## Quick Start

There are two options for getting your feet wet with Mercator.

### Docker

There is a docker image that is able to scan your AWS infrastructure and generate a graph of all the related entities.  To run it:


```bash
# Pull the latest image
$ docker pull lendingclub/mercator-demo
```

```bash
# Run the container
$ docker run -it -p 7474:7474 -p 7687:7687 -v ~/.aws:/root/.aws lendingclub/mercator-demo
```

The container will fire up a neo4j database inside the container and then invoke Mercator against AWS.

In order for this to work, you need to have read-only credentials in your ~/.aws directory.  When you launch the 
container with the command above, it will make your credentials available to Mercator running inside the container. 
It will use those credentials to perform the scan of your AWS infrastructure.

Once this is up and running, point your browser to http://localhost:7474

You can then run queries against Neo4j.

### Build/Run From Source

If you don't like the Docker approach and you can install Neo4j (not hard) and run the same AWS demo with gradle.

```bash
$ cd mercator-demo
$ ../gradlew run
```

It will use the AWS credentials stored in $HOME/.aws and scan your AWS infrasturcutre.  It will connect to Neo4j at
bolt://localhost:7687

## Usage

### Core Configuration

The [Projector](https://github.com/LendingClub/mercator/blob/master/mercator-core/src/main/java/org/lendingclub/mercator/core/Projector.java) class is the cornerstone of the
Mercator project.  It exposes configuration and a client for interacting with Neo4j.

Mercator doesn't use Spring, Dagger, Guice or any other DI framework.  That is your choice.  Mercator is intended to be simple and straightorward to use.

To create a Projector instance that connects to Neo4j at bolt://localhost:7687 with no username or password:

```java
Projector projector = new Projector.Builder.build();
```

To use a different URL:

```java
Projector projector = new Projector.Builder().withUrl("bolt://myserver:7687").build();
```

To provide credentials:

```java
Projector p = new Projector.Builder()
    .withUrl("bolt://myserver:7687")
    .withUsername("scott")
    .withPassword("tiger")
    .build();
```

If you need more control over the underlying neo4j Driver config:

```java
Projector p = new Projector.Builder()
    .withNeoRxConfig(cfg->{
        Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "neo4j" ) );
        cfg.withDriver(driver);
    })
    .build();
```

## AWS

Scanning an AWS region involves running something like the following:

```java
Projector projector = new Projector.Builder().build();

AllEntityScanner scanner = projector
    .createBuilder(AWSScannerBuilder.class)
    .withRegion(Regions.US_WEST_2)
    .build(AllEntityScanner.class);
scanner.scan();
```

This will:

1. Use the credentials found in ```${HOME}/.aws/credentials``` 
2. Construct an AWS client
3. Connect to the ```US_WEST_2``` region
4. Enumerate each entity and build a Neo4j graph model

After the scanner has been constructed, it can be used indefinitely.

### Credentials

A custom CredendialsProvider can be passed to the builder using `withCredentials(AWSCredentialsProvider)`.

As a convenience it is also possible to assume a role using the `DefaultAWSCredentialsProviderChain`.  You can 
always do this yourself.  I added it here because I tend to forget how to do it and having it in fluent 
form during development is very useful: 

```java
new AWSScannerBuilder()
	.withProjector(projector)
	.withAssumeRoleCredentials("arn:aws:iam::111222333444:role/my-assumed-role", "foo")
				.withRegion(Regions.US_WEST_2).build(ELBScanner.class).scan();
```

## VMWare

Mercator can build a graph of VMWare entities with the following bit of code:

```java
Projector projector = new Projector.Builder().build();

VMWareScanner scanner = projector.createBuilder(VMWareScannerBuilder.class)
			.withUrl("https://myvcenter.example.com/sdk")
			.withUsername("myusername")
			.withPassword("mypassword").build();

scanner.scan();
```

## GitHub

Both public GitHub and GitHub Enterprise are supported:

```java

GitHubScanner scanner = projector
	.createBuilder(GitHubScannerBuilder.class)
    .withToken("oauthtoken").build();

scanner.scanOrganization("Apache");
```

OAuth2, Username/Password, and Anonymous access are all supported.


## Jenkins

Mercator will not only scan Jenkins, but it will create relationships to GitHub repos as well!

```java
JenkinsScanner scanner = projector
    .createBuilder(JenkinsScannerBuilder.class).withUrl("https://jenkins.example.com")
    .withUsername("myusername").withPassword("mypassword").build();

scanner.scan();
```

## Docker

Mercator can talk to a Docker daemon to ingest Docker Swarm Services, Hosts, Tasks and Swarms.  

```java
projector.createBuilder(DockerScannerBuilder.class).build();
```

The underlying docker client can be configured using a Consumer callback.

```java
DockerScanner ds = p.createBuilder(DockerScannerBuilder.class).withConfig(cfg->{
	cfg.withDockerHost("tcp://my-docker-host.tld:2376")
	.withDockerTlsVerify(true)
	.withDockerCertPath("/home/user/.docker/certs")
	.withDockerConfig("/home/user/.docker")
	.withApiVersion("1.23")
	.withRegistryUrl("https://index.docker.io/v1/")
	.withRegistryUsername("dockeruser")
	.withRegistryPassword("ilovedocker")
	.withRegistryEmail("dockeruser@github.com");
	}).build();
```

## Cisco UCS

Mercator will scan UCS Manager to build relationships between:

* Blade Chassis
* Blades
* Rack Servers
* Server Service Profiles
* Fabric Interconnects
* Fabric Extenders

```java
projector.createBuilder(UCSScannerBuilder.class)
	.withUrl("https://usermanager.example.com/nuova")
	.withUsername("myusername")
	.withPassword("mypassword")
	.withCertValidationEnabled(true)
	.build()
	.scan();
```
