/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.outage.filter;

import javax.servlet.ServletContext;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.opennms.web.filter.EqualsFilter;
import org.opennms.web.filter.SQLType;

/**
 * Encapsulates all node filtering functionality.
 *
 * @author <a href="mailto:ronald.roskens@gmail.com">Ronald Roskens</a>
 * @author <a href="http://www.opennms.org/">OpenNMS</a>
 * @since 1.12.2
 */
public class ForeignSourceFilter extends EqualsFilter<String> {
    /** Constant <code>TYPE="foreignsource"</code> */
    public static final String TYPE = "foreignsource";
    private ServletContext m_servletContext;

    /**
     * Constructor for ForeignSourceFilter.
     *
     * @param foreignSource a {@link java.lang.String} object.
     */
    public ForeignSourceFilter(String foreignSource, ServletContext servletContext) {
        super(TYPE, SQLType.STRING, "OUTAGES.NODEID", "NODE.foreignSource", foreignSource);
        m_servletContext = servletContext;
    }

    /** {@inheritDoc} */
    @Override
    public String getSQLTemplate() {
        return " " + getSQLFieldName() + " in (SELECT DISTINCT NODE.nodeID FROM NODE WHERE NODE.foreignSource=%s)";
    }

    /** {@inheritDoc} */
    @Override
    public Criterion getCriterion() {
        return Restrictions.sqlRestriction(" {alias}.nodeid in (SELECT DISTINCT NODE.nodeID FROM NODE WHERE NODE.foreignSource=?)", getValue(), StringType.INSTANCE);
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        return ("<ForeignSourceFilter: " + this.getDescription() + ">");
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof ForeignSourceFilter)) return false;
        return (this.toString().equals(obj.toString()));
    }
}
