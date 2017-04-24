package org.lendingclub.mercator.docker;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lendingclub.mercator.core.MercatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

public class DockerClientSupplier implements Supplier<DockerClient> {

	private static final String CACHE_KEY="singleton";
	static Logger logger = LoggerFactory.getLogger(DockerClientSupplier.class);
	//static AtomicInteger counter = new AtomicInteger(0);
	Builder builder;
	
	Cache<String,DockerClient> cache = CacheBuilder.newBuilder().expireAfterWrite(10,TimeUnit.MINUTES).build();
	protected DockerClientSupplier() {
		
	}
	public String getName() {
		return builder.name;
	}

	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", getName()).toString();
	}

	public Builder newBuilder() {
		Builder nb = new Builder();
		nb.configList.addAll(builder.configList);
		nb.name = builder.name;
		return nb;
	}
	
	public static class Builder {
		List<Consumer<com.github.dockerjava.core.DefaultDockerClientConfig.Builder>> configList = Lists.newArrayList();

		AtomicReference<DockerClientSupplier> clientRef = new AtomicReference<DockerClientSupplier>(null);
	
		String name;

		public void assertNotImmutable() {
			if (clientRef.get()!=null) {
				throw new IllegalStateException("build() has already been called");
			}
		}
		public <T extends Builder> T withDockerConfigDir(File path) {
			assertNotImmutable();
			Optional<JsonNode> x =  DockerScannerBuilder.loadDockerConfig(path);
			if (!x.isPresent()) {
				throw new MercatorException("could not load config from: "+path);
			}

			return (T) withClientConfig(x.get());
		}

		public <T extends Builder> T withClientConfig(JsonNode n) {
			assertNotImmutable();
		
			String host = n.path("DOCKER_HOST").asText();
			if (!Strings.isNullOrEmpty(host)) {
		
				withDockerHost(host);
			}
			
			String certPath = n.path("DOCKER_CERT_PATH").asText();
			if (!Strings.isNullOrEmpty(certPath)) {
				withBuilderConfig(cfg ->{
					cfg.withDockerCertPath(certPath);
				});
			}
			String verify = n.path("DOCKER_TLS_VERIFY").asText();
			if (!Strings.isNullOrEmpty(verify)) {
				if (verify.trim().toLowerCase().equals("1")) {
					withBuilderConfig(cfg ->{
					
						cfg.withDockerTlsVerify(true);
					});
					
				}
				else {
					withBuilderConfig(cfg ->{
						cfg.withDockerTlsVerify(false);
					});
				}
			}
			return (T) this;
		}
		public <T extends Builder> T withCertPath(String path) {
			assertNotImmutable();
			return withCertPath(new File(path));
		}

		public <T extends Builder> T withLocalEngine() {
			assertNotImmutable();
			return withDockerHost("unix:///var/run/docker.sock");


		}

		public <T extends Builder> T withCertPath(File path) {
			assertNotImmutable();
			return (T) withBuilderConfig(b -> {
				b.withDockerCertPath(path.getAbsolutePath());
			});
		}



		public <T extends Builder> T withDockerHost(String host) {
			assertNotImmutable();
			return withBuilderConfig(cfg->{
				cfg.withDockerHost(host);
			});
		
		}
		public <T extends Builder> T withName(String name) {
			assertNotImmutable();
			this.name = name;
			return (T) this;
		}

		public <T extends Builder> T withBuilderConfig(
				Consumer<com.github.dockerjava.core.DefaultDockerClientConfig.Builder> consumer) {
			assertNotImmutable();
			configList.add(consumer);
			return (T) this;
		}

		public synchronized <T extends DockerClientSupplier> T build() {
			assertNotImmutable();
			if (Strings.isNullOrEmpty(name)) {
				throw new IllegalStateException("withName() must be set");
			}
			DockerClientSupplier cs = new DockerClientSupplier();
			cs.builder = this;
			clientRef.set(cs);
			return (T) cs;
		}
	}

	protected DockerClient create() {
		com.github.dockerjava.core.DefaultDockerClientConfig.Builder b = DefaultDockerClientConfig
				.createDefaultConfigBuilder();

		for (Consumer<com.github.dockerjava.core.DefaultDockerClientConfig.Builder> consumer : this.builder.configList) {
			consumer.accept(b);
		}
		
	
		
		DefaultDockerClientConfig cfg = b.build();

		DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
				  .withReadTimeout(3000)
				  .withConnectTimeout(3000)
				  .withMaxTotalConnections(100)
				  .withMaxPerRouteConnections(10);
		return DockerClientBuilder.getInstance(cfg).withDockerCmdExecFactory(dockerCmdExecFactory).build();
	}

	public void reset() {
		cache.invalidateAll();
	}
	@Override
	public DockerClient get() {
		DockerClient client = cache.getIfPresent(CACHE_KEY);
		if (client!=null) {
			return client;
		}
		client = create();
		cache.put(CACHE_KEY, client);
		return client;
	}

}
