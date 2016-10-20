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

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class WundaBlauServerResourcesPreloader {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            WundaBlauServerResourcesPreloader.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerResourcesPreloader object.
     */
    private WundaBlauServerResourcesPreloader() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     */
    public void loadAll() {
        boolean error = false;
        for (final WundaBlauServerResources resource : WundaBlauServerResources.values()) {
            try {
                resource.loadWithCachedServerResourcesLoader();
            } catch (final Exception ex) {
                LOG.warn("Exception while loading resource from the resources base path.", ex);
                error = true;
            }
        }

        if (error) {
            LOG.warn("Not all server resources could be loaded !");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static WundaBlauServerResourcesPreloader getInstance() {
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

        private static final WundaBlauServerResourcesPreloader INSTANCE = new WundaBlauServerResourcesPreloader();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}