/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.gwt.wysiwyg.client.plugin.separator;

import org.xwiki.gwt.wysiwyg.client.plugin.Plugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPluginFactory;

/**
 * Factory for {@link SeparatorPlugin}.
 * 
 * @version $Id: dbc22283b56e9255ba0aa7732671c50d1b2b232f $
 */
public final class RTSeparatorPluginFactory extends AbstractPluginFactory
{
    /**
     * The singleton factory instance.
     */
    private static RTSeparatorPluginFactory instance;

    /**
     * Default constructor.
     */
    private RTSeparatorPluginFactory()
    {
        super("rt-separator");
    }

    /**
     * @return the singleton factory instance.
     */
    public static synchronized RTSeparatorPluginFactory getInstance()
    {
        if (instance == null) {
            instance = new RTSeparatorPluginFactory();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPluginFactory#newInstance()
     */
    public Plugin newInstance()
    {
        return new RTSeparatorPlugin();
    }
}
