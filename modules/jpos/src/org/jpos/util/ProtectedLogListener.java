/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2007 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.util;

import java.util.Vector;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

/**
 * Protects selected fields from LogEvents.
 *
 * ProtectedLogListener acts like a filter for Event logs,
 * it should be defined _before_ other standard LogListeners
 * such as SimpleLogListener or RotateLogListeners.<br>
 * i.e.
 * <pre>
 * <logger name="qsp">
 *   <log-listener class="org.jpos.util.SimpleLogListener"/>
 *   <log-listener class="org.jpos.util.ProtectedLogListener">
 *     <property name="protect" value="2 35 45 55" />
 *     <property name="wipe"    value="48" />
 *   </log-listener>
 *   <log-listener class="org.jpos.util.RotateLogListener">
 *     <property name="file" value="/tmp/qsp.log" />
 *     <property name="window" value="86400" />
 *     <property name="copies" value="5" />
 *     <property name="maxsize" value="1000000" />
 *   </log-listener>
 * </logger>
 * </pre>
 * 
 * Order is important. In the previous example SimpleLogListener
 * will dump unprotected LogEvents while RotateLogListener will
 * dump protected ones (for selected fields)
 *
 * @author <a href="mailto:apr@cs.com.uy">Alejandro P. Revilla</a>
 * @version $Revision$ $Date$
 * @see org.jpos.core.Configurable
 * @since jPOS 1.3.3
 */
public class ProtectedLogListener implements LogListener, Configurable
{
    int[] protectFields = null;
    int[] wipeFields    = null;
    Configuration cfg   = null;

    public ProtectedLogListener () {
        super();
    }

   /**
    * Configure this ProtectedLogListener<br>
    * Properties:<br>
    * <ul>
    *  <li>[protect]   blank separated list of fields to be protected
    *  <li>[wipe]      blank separated list of fields to be wiped
    * </ul>
    * @param cfg Configuration 
    * @throws ConfigurationException
    */
    public void setConfiguration (Configuration cfg)
        throws ConfigurationException
    {
        this.cfg = cfg;
        protectFields = ISOUtil.toIntArray (cfg.get ("protect", ""));
        wipeFields    = ISOUtil.toIntArray (cfg.get ("wipe", ""));
    }
    public synchronized LogEvent log (LogEvent ev) {
        Vector payLoad = ev.getPayLoad();
        int size = payLoad.size();
        for (int i=0; i<size; i++) {
            Object obj = payLoad.elementAt (i);
            if (obj instanceof ISOMsg) {
                ISOMsg m = (ISOMsg) ((ISOMsg) obj).clone();
                try {
                    checkProtected (m);
                    checkHidden (m);
                } catch (ISOException e) { 
                    ev.addMessage (e);
                }
                payLoad.setElementAt (m, i);
            }
        }
        return ev;
    }
    private void checkProtected (ISOMsg m) throws ISOException {
        for (int i=0; i<protectFields.length; i++) {
            int f = protectFields[i];
            if (m.hasField (f))
                m.set (new ISOField (f, ISOUtil.protect (m.getString(f))));
        }
    }
    private void checkHidden (ISOMsg m) throws ISOException {
        for (int i=0; i<wipeFields.length; i++) {
            int f = wipeFields[i];
            if (m.hasField (f))
                m.set (new ISOField (f, "[WIPED]"));
        }
    }
}
