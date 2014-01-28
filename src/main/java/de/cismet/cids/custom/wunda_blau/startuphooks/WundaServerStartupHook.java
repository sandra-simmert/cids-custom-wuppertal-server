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
package de.cismet.cids.custom.wunda_blau.startuphooks;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.DomainServerStartupHook;

import de.cismet.cids.custom.utils.butler.ButlerProductGenerator;
import de.cismet.cids.custom.utils.nas.NASProductGenerator;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationService;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class WundaServerStartupHook implements DomainServerStartupHook {

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        /*
         * this class is used to guarantee that all classes that need to loads resources from the res.jar are
         * initialized during server startup to avoid that these resources are loaded from an eventually changed jar,
         * which will cause an vm crash or Exception
         */
        NASProductGenerator.instance();
        ButlerProductGenerator.getInstance();
        PointNumberReservationService.instance();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}