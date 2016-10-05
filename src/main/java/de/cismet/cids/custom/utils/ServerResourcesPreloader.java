/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;
import de.cismet.cids.utils.serverresources.JasperReportServerResources;
import de.cismet.cids.utils.serverresources.TextServerResources;
import de.cismet.cids.utils.serverresources.TruetypeFontServerResources;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class ServerResourcesPreloader {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerResourcesPreloader object.
     */
    private ServerResourcesPreloader() {
        final CachedServerResourcesLoader loader = CachedServerResourcesLoader.getInstance();
        for (final JasperReportServerResources resource : JasperReportServerResources.values()) {
            loader.loadJasperReportResource(resource);
        }
        for (final TextServerResources resource : TextServerResources.values()) {
            loader.loadTextResource(resource);
        }
        for (final TruetypeFontServerResources truetypeFontResource : TruetypeFontServerResources.values()) {
            loader.loadTruetypeFontResource(truetypeFontResource);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ServerResourcesPreloader getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final ServerResourcesPreloader INSTANCE = new ServerResourcesPreloader();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
