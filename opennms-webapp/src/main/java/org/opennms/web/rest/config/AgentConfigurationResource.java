package org.opennms.web.rest.config;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opennms.core.config.api.ConfigurationResource;
import org.opennms.core.config.api.ConfigurationResourceException;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.config.agents.AgentResponse;
import org.opennms.netmgt.config.agents.AgentResponseCollection;
import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Filter;
import org.opennms.netmgt.config.collectd.Service;
import org.opennms.netmgt.dao.api.MonitoredServiceDao;
import org.opennms.netmgt.filter.FilterDao;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.spi.resource.PerRequest;

@Component
@PerRequest
@Scope("prototype")
public class AgentConfigurationResource implements InitializingBean {
    private static Logger LOG = LoggerFactory.getLogger(AgentConfigurationResource.class);

    @Resource(name="collectd-configuration.xml")
    private ConfigurationResource<CollectdConfiguration> m_collectdConfigurationResource;

    @Autowired
    private FilterDao m_filterDao;

    @Autowired
    private MonitoredServiceDao m_monitoredServiceDao;

    @Autowired
    private SnmpAgentConfigFactory m_agentConfigFactory;

    @Context
    private ResourceContext m_context;

    @Context 
    private UriInfo m_uriInfo;

    public void setCollectdConfigurationResource(final ConfigurationResource<CollectdConfiguration> resource) {
        m_collectdConfigurationResource = resource;
    }

    public void setFilterDao(final FilterDao dao) {
        m_filterDao = dao;
    }

    public void setMonitoredServiceDao(final MonitoredServiceDao dao) {
        m_monitoredServiceDao = dao;
    }

    public void setAgentConfigFactory(final SnmpAgentConfigFactory factory) {
        m_agentConfigFactory = factory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(m_collectdConfigurationResource, "CollectdConfigurationResource must not be null.");
        Assert.notNull(m_filterDao, "FilterDao must not be null.");
        Assert.notNull(m_monitoredServiceDao, "MonitoredServiceDao must not be null.");
        Assert.notNull(m_agentConfigFactory, "SnmpConfigDao must not be null.");
    }

    @GET
    @Path("{filterName}/{serviceName}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML})
    public Response getAgentsXml(@PathParam("filterName") final String filterName, @PathParam("serviceName") final String serviceName) throws ConfigurationResourceException {
        final List<AgentResponse> responses = getResponses(filterName, serviceName);

        if (responses.size() == 0) {
            return Response.noContent().build();
        }

        return Response.ok(new AgentResponseCollection(responses)).build();
    }

    @GET
    @Path("{filterName}/{serviceName}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAgentsJson(@PathParam("filterName") final String filterName, @PathParam("serviceName") final String serviceName) throws ConfigurationResourceException {
        final List<AgentResponse> responses = getResponses(filterName, serviceName);

        if (responses.size() == 0) {
            return Response.noContent().build();
        }

        return Response.ok(new GenericEntity<List<AgentResponse>>(responses){}).build();
    }

    protected List<AgentResponse> getResponses(final String filterName, final String serviceName) throws ConfigurationResourceException {
        LOG.debug("getAgentsForService(): filterName={}, serviceName={}", filterName, serviceName);

        if (filterName == null || serviceName == null) {
            throw new IllegalArgumentException("You must specify a filter name and service name!");
        }

        final Filter filter = m_collectdConfigurationResource.get().getFilter(filterName);
        if (filter == null) {
            LOG.warn("No filter matching {} could be found.", filterName);
            throw new WebApplicationException(404);
        }

        final List<InetAddress> addresses = m_filterDao.getActiveIPAddressList(filter.getContent());
        LOG.debug("Matched {} IP addresses for filter {}", addresses == null? 0 : addresses.size(), filterName);

        if (addresses == null || addresses.size() == 0) {
            return Collections.emptyList();
        }

        final CriteriaBuilder builder = new CriteriaBuilder(OnmsMonitoredService.class);
        builder.createAlias("ipInterface", "iface");
        builder.createAlias("serviceType", "type");
        builder.in("iface.ipAddress", addresses);
        builder.eq("type.name", serviceName);
        final List<OnmsMonitoredService> services = m_monitoredServiceDao.findMatching(builder.toCriteria());
        int defaultPort = -1;

        // TODO: We shouldn't have to hardcode like this; what's the right way to know the port to return?
        final CollectdConfiguration collectdConfiguration = m_collectdConfigurationResource.get();
        org.opennms.netmgt.config.collectd.Package pack = collectdConfiguration.getPackage(filterName);
        if (pack == null) {
            for (final org.opennms.netmgt.config.collectd.Package p : collectdConfiguration.getPackages()) {
                if (filterName.equals(p.getFilter().getName())) {
                    pack = p;
                    break;
                }
            }
        }
        if (pack != null) {
            final Service svc = pack.getService(serviceName);
            final String port = svc.getParameter("port");
            if (port != null) {
                try {
                    defaultPort = Integer.valueOf(port);
                } catch (final NumberFormatException e) {
                    LOG.debug("Unable to turn port {} from service {} into a number.", port, serviceName);
                }
            }
        }

        final List<AgentResponse> responses = new ArrayList<AgentResponse>();

        for (final OnmsMonitoredService service : services) {
            final InetAddress ipAddress = service.getIpAddress();

            int port = defaultPort;
            if ("SNMP".equals(serviceName)) {
                final SnmpAgentConfig config = m_agentConfigFactory.getAgentConfig(ipAddress);
                if (config != null) {
                    port = config.getPort();
                }
            }

            responses.add(new AgentResponse(ipAddress, port, service.getServiceName()));
        }
        return responses;
    }
}
