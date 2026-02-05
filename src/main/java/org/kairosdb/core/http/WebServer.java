/*
 * Copyright 2016 KairosDB Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.kairosdb.core.http;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.kairosdb.core.KairosDBService;
import org.kairosdb.core.exception.KairosDBException;
import org.kairosdb.core.http.rest.AdminResource;
import org.kairosdb.core.http.rest.FeaturesResource;
import org.kairosdb.core.http.rest.MetadataResource;
import org.kairosdb.core.http.rest.MetricsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.kairosdb.util.Preconditions.requireNonNullOrEmpty;


public class WebServer implements KairosDBService
{
	public static final Logger logger = LoggerFactory.getLogger(WebServer.class);
	public static final int LOG_RETAIN_DAYS = 30;

	public static final String JETTY_ADDRESS_PROPERTY = "kairosdb.jetty.address";
	public static final String JETTY_PORT_PROPERTY = "kairosdb.jetty.port";
	public static final String JETTY_WEB_ROOT_PROPERTY = "kairosdb.jetty.static_web_root";
	public static final String JETTY_SOCKET_IDLE_TIMEOUT = "kairosdb.jetty.socket_idle_timeout";
	public static final String JETTY_SSL_PORT = "kairosdb.jetty.ssl.port";
	public static final String JETTY_SSL_PROTOCOLS = "kairosdb.jetty.ssl.protocols";
	public static final String JETTY_SSL_CIPHER_SUITES = "kairosdb.jetty.ssl.cipherSuites";
	public static final String JETTY_SSL_KEYSTORE_PATH = "kairosdb.jetty.ssl.keystore.path";
	public static final String JETTY_SSL_KEYSTORE_PASSWORD = "kairosdb.jetty.ssl.keystore.password";
	public static final String JETTY_SSL_TRUSTSTORE_PATH = "kairosdb.jetty.ssl.truststore.path";
	public static final String JETTY_THREADS_QUEUE_SIZE_PROPERTY = "kairosdb.jetty.threads.queue_size";
	public static final String JETTY_THREADS_MIN_PROPERTY = "kairosdb.jetty.threads.min";
	public static final String JETTY_THREADS_MAX_PROPERTY = "kairosdb.jetty.threads.max";
	public static final String JETTY_THREADS_KEEP_ALIVE_MS_PROPERTY = "kairosdb.jetty.threads.keep_alive_ms";
	public static final String JETTY_SHOW_STACKTRACE = "kairosdb.jetty.show_stacktrace";
	public static final String JETTY_AUTH_MODULE_NAME = "kairosdb.jetty.auth_module_name";
	public static final String JETTY_REQUEST_LOGGING_ENABLED = "kairosdb.jetty.request_logging.enabled";
	public static final String JETTY_REQUEST_LOGGING_RETAIN_DAYS = "kairosdb.jetty.request_logging.retain_days";
	public static final String JETTY_REQUEST_LOGGING_IGNORE_PATHS = "kairosdb.jetty.request_logging.ignore_paths";
	public static final String AUTH_PROPS_SYSTEM_PROPERTY = "kairosdb.auth.props";
	public static final String KAIROSDB_DEFAULT_HOME = "/opt/kairosdb";
	public static final String[] AUTH_PROPS_DEFAULT_PATHS = {
		"conf/auth/secrets/auth.props",
		"conf/auth/auth.props"
	};


	private InetAddress m_address;
	private int m_port;
	private String m_webRoot;
	private Server m_server;
	private final int m_idleTimeout;
	private int m_sslPort;
	private String[] m_cipherSuites;
	private String[] m_protocols;
	private String m_keyStorePath;
	private String m_keyStorePassword;
	private String m_trustStorePath = null;
	private ExecutorThreadPool m_pool;
	private boolean m_showStacktrace;
	private String m_authModuleName = null;
	private int m_requestLoggingRetainDays = LOG_RETAIN_DAYS;
	private boolean m_requestLoggingEnabled;
	private String[] m_loggingIgnorePaths;
	
	private final Injector m_injector;


	public WebServer(int port, String webRoot)
			throws UnknownHostException
	{
		this(null, port, webRoot, 120000, null);
	}
	
	public WebServer(String address, int port, String webRoot, int idleTimeout)
			throws UnknownHostException
	{
		this(address, port, webRoot, idleTimeout, null);
	}

	@Inject
	public WebServer(@Named(JETTY_ADDRESS_PROPERTY) String address,
			@Named(JETTY_PORT_PROPERTY) int port,
			@Named(JETTY_WEB_ROOT_PROPERTY) String webRoot,
			@Named(JETTY_SOCKET_IDLE_TIMEOUT) int idleTimeout,
			Injector injector)
			throws UnknownHostException
	{
		requireNonNull(webRoot);

		m_port = port;
		m_webRoot = webRoot;
		m_address = InetAddress.getByName(address);
		m_idleTimeout = idleTimeout;
		m_injector = injector;
	}

	@Inject(optional = true)
	public void setSSLSettings(@Named(JETTY_SSL_PORT) int sslPort,
	                           @Named(JETTY_SSL_KEYSTORE_PATH) String keyStorePath,
	                           @Named(JETTY_SSL_KEYSTORE_PASSWORD) String keyStorePassword)
	{
		m_sslPort = sslPort;
		m_keyStorePath = requireNonNullOrEmpty(keyStorePath);
		m_keyStorePassword = requireNonNullOrEmpty(keyStorePassword);
	}

	@Inject(optional = true)
	public void setSSLSettings(@Named(JETTY_SSL_TRUSTSTORE_PATH) String truststorePath)
	{
		m_trustStorePath = requireNonNullOrEmpty(truststorePath);
	}

	@Inject(optional = true)
	public void setSSLCipherSuites(@Named(JETTY_SSL_CIPHER_SUITES) String cipherSuites)
	{
		requireNonNull(cipherSuites);
		m_cipherSuites = cipherSuites.split("\\s*,\\s*");
	}

	@Inject(optional = true)
	public void setSSLProtocols(@Named(JETTY_SSL_PROTOCOLS) String protocols)
	{
		m_protocols = protocols.split("\\s*,\\s*");
	}

	@Inject(optional = true)
	public void setThreadPool(@Named(JETTY_THREADS_QUEUE_SIZE_PROPERTY) int maxQueueSize,
	                            @Named(JETTY_THREADS_MIN_PROPERTY) int minThreads,
	                            @Named(JETTY_THREADS_MAX_PROPERTY) int maxThreads,
	                            @Named(JETTY_THREADS_KEEP_ALIVE_MS_PROPERTY) long keepAliveMs)
	{
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(maxQueueSize);
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(minThreads, maxThreads, keepAliveMs, TimeUnit.MILLISECONDS, queue);
		m_pool = new ExecutorThreadPool(threadPoolExecutor);
	}

	@Inject
	public void setJettyShowStacktrace(@Named(JETTY_SHOW_STACKTRACE) boolean showStacktrace) {
		m_showStacktrace = showStacktrace;
	}

	@Inject(optional = true)
	public void setJettyAuthModuleName(@Named(JETTY_AUTH_MODULE_NAME) String moduleName)
	{
		m_authModuleName = moduleName;
	}

	@Inject(optional = true)
	public void setJettyRequestLoggingEnabled(@Named(JETTY_REQUEST_LOGGING_ENABLED) String loggingEnabled)
	{
		m_requestLoggingEnabled = Boolean.parseBoolean(loggingEnabled);
	}

	@Inject(optional = true)
	public void setJettyRequestLoggingRetainDays(@Named(JETTY_REQUEST_LOGGING_RETAIN_DAYS) String retainDays)
	{
		m_requestLoggingRetainDays = Integer.parseInt(retainDays);
	}

	@Inject(optional = true)
	void setJettyRequestLoggingIgnorePaths(@Named(JETTY_REQUEST_LOGGING_IGNORE_PATHS) String ignorePaths)
	{
		Splitter splitter = Splitter.on(",");
		CharMatcher cm =  CharMatcher.anyOf("[]").or(CharMatcher.whitespace());
		splitter = splitter.trimResults(cm);
		List<String> ignorePathsList = splitter.splitToList(ignorePaths);
		m_loggingIgnorePaths = ignorePathsList.toArray(new String[ignorePathsList.size()]);
	}

	@Override
	public void start() throws KairosDBException
	{
		try
		{
			if (m_pool != null)
				m_server = new Server(m_pool);
			else
				m_server = new Server();

			if (m_port > 0)
			{
				ServerConnector http = new ServerConnector(m_server);
				http.setHost(m_address.getHostName());
				http.setPort(m_port);
				http.setIdleTimeout(m_idleTimeout);
				m_server.addConnector(http);
			}

			if (m_keyStorePath != null && !m_keyStorePath.isEmpty())
				initializeSSL();

			ServletContextHandler servletContextHandler = new ServletContextHandler();
			servletContextHandler.setContextPath("/");
			
			if (m_authModuleName != null)
			{
				servletContextHandler.setSecurityHandler(initializeAuth());
			}

			// Configure Jersey with JAX-RS resources
			ResourceConfig resourceConfig = new ResourceConfig();
			
			// Get resource instances from Guice injector if available
			if (m_injector != null)
			{
				resourceConfig.register(new GuiceFeature(m_injector));
				resourceConfig.register(m_injector.getInstance(MetricsResource.class));
				resourceConfig.register(m_injector.getInstance(MetadataResource.class));
				resourceConfig.register(m_injector.getInstance(FeaturesResource.class));
				resourceConfig.register(m_injector.getInstance(AdminResource.class));
				// Register exception mappers
				try
				{
					resourceConfig.register(m_injector.getInstance(org.kairosdb.core.http.exceptionmapper.InvalidServerTypeExceptionMapper.class));
				}
				catch (Exception e)
				{
					// Mapper might not be bound
				}
			}
			else
			{
				// Fallback: register classes for Jersey to instantiate
				resourceConfig.register(MetricsResource.class);
				resourceConfig.register(MetadataResource.class);
				resourceConfig.register(FeaturesResource.class);
				resourceConfig.register(AdminResource.class);
				resourceConfig.register(org.kairosdb.core.http.exceptionmapper.InvalidServerTypeExceptionMapper.class);
			}
			
			// Add logging filter if available
			if (m_injector != null)
			{
				try
				{
					resourceConfig.register(m_injector.getInstance(LoggingFilter.class));
				}
				catch (Exception e)
				{
					// LoggingFilter might not be bound
				}
			}
			
			// Create Jersey servlet - serve at /api/* since resources have @Path("/v1")
			ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));
			servletContextHandler.addServlet(jerseyServlet, "/api/*");
			
			// Default servlet for unhandled requests
			ServletHolder defaultServlet = new ServletHolder("default", DefaultServlet.class);
			servletContextHandler.addServlet(defaultServlet, "/");

			GzipHandler gzipHandler = new GzipHandler();
			gzipHandler.addIncludedMimeTypes("application/json");
			gzipHandler.addIncludedMethods("GET", "POST");
			gzipHandler.addIncludedPaths("/*");
			gzipHandler.setMinGzipSize(1);
			
			// ResourceHandler for static content
			ResourceHandler resourceHandler = new ResourceHandler();
			File webRootFile = new File(m_webRoot);
			if (webRootFile.exists() && webRootFile.isDirectory())
			{
				resourceHandler.setBaseResource(ResourceFactory.root().newResource(webRootFile.toPath()));
				resourceHandler.setDirAllowed(true);
				resourceHandler.setWelcomeFiles("index.html");
			}

			// Chain handlers: gzip -> resourceHandler -> servletContext
			gzipHandler.setHandler(resourceHandler);
			resourceHandler.setHandler(servletContextHandler);

			m_server.setHandler(gzipHandler);
			if(m_requestLoggingEnabled)
				initializeJettyRequestLogging();

			m_server.start();
		}
		catch (Exception e)
		{
			throw new KairosDBException(e);
		}
	}

	@Override
	public void stop()
	{
		try
		{
			if (m_server != null)
			{
				m_server.stop();
				m_server.join();
			}
		}
		catch (Exception e)
		{
			logger.error("Error stopping web server", e);
		}
	}

	public InetAddress getAddress()
    {
        return m_address;
    }

	private void initializeSSL()
	{
		logger.info("Using SSL");
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(m_sslPort);
		httpConfig.addCustomizer(new SecureRequestCustomizer());

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(m_keyStorePath);
		sslContextFactory.setKeyStorePassword(m_keyStorePassword);
		if (m_trustStorePath != null && !m_trustStorePath.isEmpty())
			sslContextFactory.setTrustStorePath(m_trustStorePath);

		if (m_cipherSuites != null && m_cipherSuites.length > 0)
			sslContextFactory.setIncludeCipherSuites(m_cipherSuites);

		if (m_protocols != null && m_protocols.length > 0)
			sslContextFactory.setIncludeProtocols(m_protocols);

		ServerConnector https = new ServerConnector(m_server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpConfig));
		https.setPort(m_sslPort);
		https.setIdleTimeout(m_idleTimeout);
		m_server.addConnector(https);
	}

	static File findConfigFile(String systemProperty, String[] defaultPaths, String homeEnvVar, String defaultHome)
	{
		String overridePath = systemProperty != null ? System.getProperty(systemProperty) : null;
		String homeDir = homeEnvVar != null ? System.getenv(homeEnvVar) : null;

		List<String> searchPaths = new ArrayList<>();
		if (overridePath != null)
		{
			searchPaths.add(overridePath);
		}
		for (String defaultPath : defaultPaths)
		{
			searchPaths.add(defaultPath);
			if (homeDir != null)
			{
				searchPaths.add(homeDir + "/" + defaultPath);
			}
			if (defaultHome != null)
			{
				searchPaths.add(defaultHome + "/" + defaultPath);
			}
		}

		for (String path : searchPaths)
		{
			File file = new File(path);
			if (file.exists())
			{
				return file;
			}
		}
		return null;
	}

	private SecurityHandler initializeAuth() throws Exception
	{
		SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
		HashLoginService loginService = new HashLoginService();
		loginService.setName(m_authModuleName);

		File authPropsFile = findConfigFile(AUTH_PROPS_SYSTEM_PROPERTY, AUTH_PROPS_DEFAULT_PATHS, "KAIROSDB_HOME", KAIROSDB_DEFAULT_HOME);
		
		if (authPropsFile != null)
		{
			loginService.setConfig(ResourceFactory.root().newResource(authPropsFile.toPath()));
			logger.info("Using auth properties file: {}", authPropsFile.getAbsolutePath());
		}
		else
		{
			logger.warn("No auth properties file found. Authentication will fail for all users. " +
					"Set -D{}=<path> or create one of: {}", AUTH_PROPS_SYSTEM_PROPERTY, 
					Arrays.toString(AUTH_PROPS_DEFAULT_PATHS));
		}
		
		securityHandler.setLoginService(loginService);
		securityHandler.setAuthenticator(new BasicAuthenticator());
		
		// Allow health check endpoint without authentication
		securityHandler.put("/api/v1/health/*", Constraint.ALLOWED);
		// Require authentication for all other paths
		securityHandler.put("/*", Constraint.ANY_USER);
		
		return securityHandler;
    }

    private void initializeJettyRequestLogging()
	{
		RequestLogWriter logWriter = new RequestLogWriter("log/jetty-yyyy_mm_dd.request.log");
		CustomRequestLog requestLog = new CustomRequestLog(logWriter, CustomRequestLog.NCSA_FORMAT);
		logWriter.setAppend(true);
		logWriter.setTimeZone("UTC");
		logWriter.setRetainDays(m_requestLoggingRetainDays);
		if(m_loggingIgnorePaths != null)
			requestLog.setIgnorePaths(m_loggingIgnorePaths);
		m_server.setRequestLog(requestLog);
	}
}
