/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.distro.es;

import io.apiman.common.es.util.EsConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Starts up an embedded elasticsearch cluster.  This is useful when running
 * apiman in ES storage mode.  This takes the place of a standalone
 * elasticsearch cluster installation.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("nls")
public class Bootstrapper implements ServletContextListener {

    private static ElasticsearchContainer node;

    /**
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DistroESConfig config = new DistroESConfig();

        System.out.println("------------------------------------------------------------");
        System.out.println("Starting apiman-es.");
        System.out.println("   HTTP Ports:      " + config.getHttpPortRange());
        System.out.println("   Bind Host:       " + config.getBindHost());
        System.out.println("------------------------------------------------------------");

        String clusterName = "apiman";

        try {
            URL esDistroUrl = resolveEsDistro(sce);
            File tmpDir = new File(getTempDir());

            node = new FixedPortEsContainer(
                 "docker.elastic.co/elasticsearch/elasticsearch-oss:" + EsConstants.getEsVersion())
                 .withFixedExposedPortRange(config.getHttpPortRange())
                 .withStartupTimeout(Duration.ofHours(1))
                 .waitingFor(Wait.forHttp("/"));

            System.out.println("If you have never downloaded elasticsearch before, this may take a few moments...");

            node.start();
//
//            Builder builder = ApimanEmbeddedElastic.builder()
//                .withPort(config.getHttpPortRange())
//                .withDownloadUrl(esDistroUrl)
//                .withDownloadDirectory(tmpDir)
//                .withInstallationDirectory(tmpDir)
//                .withSetting("path.data", esHome)
//                .withCleanInstallationDirectoryOnStop(false)
//                .withSetting(PopularProperties.TRANSPORT_TCP_PORT, config.getTransportPortRange())
//                .withSetting(PopularProperties.CLUSTER_NAME, clusterName)
//                .withStartTimeout(1, TimeUnit.MINUTES);
//
//            if (config.getBindHost() != null) {
//                builder.withSetting("network.bind_host", config.getBindHost());
//            }
//
//            node = builder.build().start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("-----------------------------");
        System.out.println("apiman-es started!");
        System.out.println("-----------------------------");
    }

    private static String getTempDir() {
        if (System.getProperties().contains("jboss.server.temp.dir")) {
            return System.getProperty("jboss.server.temp.dir");
        }
        return System.getProperty("java.io.tmpdir");
    }

    private URL resolveEsDistro(ServletContextEvent sce) throws MalformedURLException {
        String systemProp = System.getProperty("apiman.es-distro");
        if (systemProp != null) {
            return new URL(systemProp);
        }

        URL url = Bootstrapper.class.getResource("embedded-elastic.properties");
        if (url == null) {
            throw new RuntimeException("embedded-elastic.properties missing.");
        } else {
            Properties allProperties = new Properties();
            try(InputStream is = url.openStream()){
                allProperties.load(is);
                String dPath = Optional
                        .ofNullable(allProperties.getProperty("apiman.es-distro"))
                        .orElseThrow(() -> new RuntimeException("apiman.es-distro not defined"));

                return sce.getServletContext().getResource(dPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
//        if (node != null) {
//            try {
//                node.stop();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        node.stop();

        System.out.println("-----------------------------");
        System.out.println("apiman-es stopped!");
        System.out.println("-----------------------------");
    }
}
