/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class Sb_maxBildnummerFetcherServerSearch extends AbstractCidsServerSearch
        implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final String searchQuery =
        "select max(bildnummer::int) from sb_stadtbild where bildnummer ~ '^\\\\d{6}$'";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final ArrayList<ArrayList> lists = ms.performCustomSearch(searchQuery, getServerConnectionContext());
                return lists;
            } catch (RemoteException ex) {
            }
        }
        return null;
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(getClass().getSimpleName());
    }
}
