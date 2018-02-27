/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * Search the kk_verfahren parent of a kk_kompensation object.
 *
 * @author   Thorsten Herter
 * @version  $Revision$, $Date$
 */
public class KkVerfahrenSearch extends AbstractCidsServerSearch implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(KkVerfahrenSearch.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String QUERY =
        "select verfahren_reference from kk_verfahren_kompensationen where kompensation = ";

    //~ Instance fields --------------------------------------------------------

    private final int kompensationId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     *
     * @param  kompensationId  DOCUMENT ME!
     */
    public KkVerfahrenSearch(final int kompensationId) {
        this.kompensationId = kompensationId;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        try {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get(DOMAIN);
            if (metaService != null) {
                final ArrayList<ArrayList> list = metaService.performCustomSearch(QUERY + kompensationId,
                        getServerConnectionContext());

                if ((list != null) && (list.size() == 1) && (list.get(0).size() == 1)) {
                    return Arrays.asList(metaService.getMetaObject(
                                getUser(),
                                (Integer)list.get(0).get(0),
                                CidsBean.getMetaClassFromTableName(DOMAIN, "kk_verfahren").getId(),
                                getServerConnectionContext()));
                } else {
                    LOG.error("active local server not found"); // NOI18N
                }
            } else {
                LOG.error("active local server not found");     // NOI18N
            }

            return null;
        } catch (final Exception ex) {
            throw new SearchException("error while loading verfahren objects", ex);
        }
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(getClass().getSimpleName());
    }
}
