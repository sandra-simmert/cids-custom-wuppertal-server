/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class SubPoiSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(SubPoiSearch.class);

    public static final String TABLE_NAME = "poi_locationinstance";
    public static final String TABLE_NAME_ARR = "poi_alt_geo_identifier_arrray";
    public static final String TABLE_NAME_ALT = "poi_alternativegeographicidentifier";
    public static final String FIELD__ID = "id";
    public static final String FIELD__NAME = "geographicidentifier";
    public static final String FIELD__ALT = "alternativegeographicidentifier";
    public static final String FIELD__ARR_POI = "number_li";
    public static final String FIELD__ARR_ALT = "alt_geo_id";
    public static final String FIELD__ALT_ID = "id";
    public static final String FIELD__ALT_NAME = "alternativegeographicidentifier";

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String QUERY_TEMPLATE = "WITH help_alt AS ("
            + "SELECT " + TABLE_NAME_ARR + "." + FIELD__ARR_POI + " AS poi_reference, "
            + "'  + ' || " + "string_agg(" + TABLE_NAME_ALT + "." + FIELD__ALT_NAME + ", ' -- '::text) AS altnamen"
            + " FROM " + TABLE_NAME_ALT 
            + " LEFT JOIN " + TABLE_NAME_ARR + " ON " + TABLE_NAME_ARR + "." + FIELD__ARR_ALT
                        + " = " + TABLE_NAME_ALT + "." + FIELD__ALT_ID
            + " GROUP BY " + TABLE_NAME_ARR + "." + FIELD__ARR_POI + ") "
            + "SELECT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_NAME + "') AS class_id, "
                + TABLE_NAME + "." + FIELD__ID + ", "
                + "COALESCE(" + TABLE_NAME + "." + FIELD__NAME + " || help_alt.altnamen,  " 
                + TABLE_NAME + "." + FIELD__NAME + ") AS name"
                + " FROM " + TABLE_NAME
                + " LEFT JOIN help_alt ON help_alt.poi_reference = " + TABLE_NAME + "." + FIELD__ALT
                + " ORDER BY " + TABLE_NAME + "." + FIELD__NAME;
    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter private Integer poiId;
    @Setter @Getter private String name;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SubPoiSearch object.
     */
    public SubPoiSearch() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  connectionContext  DOCUMENT ME!
     */
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<String> leftJoins = new ArrayList<>();
            final List<String> wheres = new ArrayList<>();
            if (getName() != null) {
                wheres.add(String.format("poi_locationinstance.id = %d", getPoiId()));
            }

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.format("WHERE %s", String.join(" OR ", wheres)) : "";
            final String query = String.format(QUERY_TEMPLATE, leftJoin, where);
        LOG.info(query);
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
            final List<MetaObjectNode> mons = new ArrayList<>();
            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String poiName = String.valueOf(al.get(2));
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, poiName, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final RemoteException ex) {
            LOG.error("error while searching for poi_locationinstance", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
