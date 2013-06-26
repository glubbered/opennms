package org.opennms.netmgt.model.topology;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * This class is a container of end points.  In the network, this
 * can be either a physical or virtual node/device/subnetwork/etc.
 */
@Entity
@Table(name="topologyElement")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class TopologyElement {
	
	private int m_id;

	private List<EndPoint> m_endpoints;

	private List<ElementIdentifier> m_identifiers;

	public TopologyElement() {
		m_endpoints = new ArrayList<EndPoint>();
		m_identifiers = new ArrayList<ElementIdentifier>();
	}
	
	@Id
    @SequenceGenerator(name="opennmsSequence", sequenceName="opennmsNxtId")
    @GeneratedValue(generator="opennmsSequence")
	public int getId() {
		return m_id;
	}

	public void setId(int id) {
		m_id = id;
	}

    @OneToMany(mappedBy="topologyElement",cascade=CascadeType.ALL)
	public List<EndPoint> getEndpoints() {
		return m_endpoints;
	}

	public void setEndpoints(List<EndPoint> endpoints) {
		m_endpoints = endpoints;
	}
	
    @OneToMany(mappedBy="topologyElement",cascade=CascadeType.ALL)
	public List<ElementIdentifier> getElementIdentifiers() {
		return m_identifiers;
	}

	public void setElementIdentifiers(List<ElementIdentifier> identifiers) {
		m_identifiers = identifiers;
	}

	public boolean hasElementIdentifier(ElementIdentifier elementidentifier) {
		return m_identifiers.contains(elementidentifier);
	}
	
	public void removeElementIdentifier(ElementIdentifier elementidentifier) {
		m_identifiers.remove(elementidentifier);
	}
	
	public void addElementIdentifier(ElementIdentifier elementidentifier) {
		if (hasElementIdentifier(elementidentifier))
			return;
		elementidentifier.setTopologyElement(this);
		m_identifiers.add(elementidentifier);
	}

	public boolean hasEndPoint(EndPoint endPoint) {
		return m_endpoints.contains(endPoint);
	}
	
	public void removeEndPoint(EndPoint endPoint) {
		m_endpoints.remove(endPoint);
	}

	public EndPoint getEndPoint(EndPoint endPoint) {
		for (EndPoint e: m_endpoints) {
			if (endPoint.equals(e)) {
				return e;
			}
		}
		return null;
	}

	public void addEndPoint(EndPoint endPoint) {
		if (hasEndPoint(endPoint))
			return;
		endPoint.setTopologyElement(this);
		m_endpoints.add(endPoint);
	}
	
	public boolean equals(Object o) {
		if ( o instanceof TopologyElement) {
			TopologyElement e = (TopologyElement) o;
			for (ElementIdentifier localElementIdentifier : getElementIdentifiers()) {
				for (ElementIdentifier oe: e.getElementIdentifiers()) {
					if (oe.equals(localElementIdentifier))
						return true;
				}
			}
		}
		return false;
	}
	
	public String toString() {
		ToStringBuilder tosb = new ToStringBuilder(this);
		for (ElementIdentifier ei: m_identifiers) {
			tosb.append("\nElementIdentifier",ei);
		}
		for (EndPoint ep: m_endpoints) {
			tosb.append("\nEndPoint",ep);
		}
		return tosb.toString();
	}
}