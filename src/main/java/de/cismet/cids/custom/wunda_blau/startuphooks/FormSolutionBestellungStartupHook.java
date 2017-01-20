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
import Sirius.server.newuser.User;
import Sirius.server.newuser.UserServer;

import java.rmi.Naming;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionsProperties;
import de.cismet.cids.custom.wunda_blau.search.actions.FormSolutionServerNewStuffAvailableAction;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class FormSolutionBestellungStartupHook implements DomainServerStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionBestellungStartupHook.class);

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        DomainServerImpl metaService = null;
                        while (metaService == null) {
                            metaService = DomainServerImpl.getServerInstance();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                            }
                        }

                        final FormSolutionServerNewStuffAvailableAction action =
                            new FormSolutionServerNewStuffAvailableAction(true);
                        action.setMetaService(DomainServerImpl.getServerInstance());
                        final Object userServer = Naming.lookup("rmi://localhost/userServer");
                        final User user = ((UserServer)userServer).getUser(
                                null,
                                null,
                                "WUNDA_BLAU",
                                FormSolutionsProperties.getInstance().getCidsLogin(),
                                FormSolutionsProperties.getInstance().getCidsPassword());
                        action.setUser(user);
                        action.execute(null);
                    } catch (final Exception ex) {
                        LOG.error("error while executing FormSolutionBestellungStartupHook", ex);
                    }
                }
            }).start();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
