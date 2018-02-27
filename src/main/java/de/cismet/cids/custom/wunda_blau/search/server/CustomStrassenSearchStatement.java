/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObjectNode;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class CustomStrassenSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(CustomStrassenSearchStatement.class);

    //~ Instance fields --------------------------------------------------------

    private final String searchString;
    private final boolean searchForStrassenschluessel;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CustomStrassenSearchStatement object.
     *
     * @param  searchString  DOCUMENT ME!
     */
    public CustomStrassenSearchStatement(final String searchString) {
        this(searchString, false);
    }

    /**
     * Creates a new CustomStrassenSearchStatement object.
     *
     * @param  searchString                 DOCUMENT ME!
     * @param  searchForStrassenschluessel  DOCUMENT ME!
     */
    public CustomStrassenSearchStatement(final String searchString, final boolean searchForStrassenschluessel) {
        this.searchString = searchString;
        this.searchForStrassenschluessel = searchForStrassenschluessel;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("search started");
            }

            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final MetaClass c = ms.getClassByTableName(getUser(), "strasse", getServerConnectionContext());

            final String sql = ""
                        + "SELECT strassenschluessel, name "
                        + "FROM strasse "
                        + "WHERE "
                        + (searchForStrassenschluessel ? ("strassenschluessel = " + searchString + "")
                                                       : ("name LIKE '%" + searchString + "%'")) + " "
                        + "ORDER BY name desc";

            final ArrayList<ArrayList> result = ms.performCustomSearch(sql, getServerConnectionContext());

            final ArrayList<MetaObjectNode> aln = new ArrayList<>();
            for (final ArrayList al : result) {
                final int id = (Integer)al.get(0);
                aln.add(new MetaObjectNode(c.getDomain(), id, c.getId()));
            }
            return aln;
        } catch (final Exception ex) {
            LOG.error("Problem", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(getClass().getSimpleName());
    }
}
