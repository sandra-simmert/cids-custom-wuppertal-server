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
import com.vividsolutions.jts.geom.Geometry;

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
public class SubObjectGeomSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(SubObjectGeomSearch.class);

    public static final String TABLE_NAME = "sub_object";
    public static final String FIELD__ID = "id";
    public static final String FIELD__NAME = "name";
    public static String distance = "0";

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String QUERY_TEMPLATE = "SELECT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_NAME + "') AS class_id, "
                + TABLE_NAME + "." + FIELD__ID + ", "
                + TABLE_NAME + "." + FIELD__NAME + " (" + distance + ")"
                + " FROM " + TABLE_NAME;

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter public Integer gebietId;
    @Setter @Getter public Geometry point;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SubObjectGeomSearch object.
     */
    public SubObjectGeomSearch() {
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
            distance = "'(0.0)'";
            String punkt = "'" + getPoint() + "'";
            final List<String> leftJoins = new ArrayList<>();
                leftJoins.add("geom ON geom.id = sub_object.fk_geom_point");
            final List<String> wheres = new ArrayList<>();
            final List<String> orderBys = new ArrayList<>();
            if (getGebietId() != null) {
                distance = String.format(" st_distance(st_geomfromtext((%s),25832), geom.geo_field)", punkt);         
                wheres.add(String.format("sub_object.id <> %d", getGebietId()));
                wheres.add("sub_object.id NOT IN (SELECT sub_object_object.sub_object_reference FROM sub_object_object)");
                orderBys.add(distance);
            //orderBys.add(String.format("CAST(%d AS integer)", distance));
            } else{
                distance = String.format(" || '(' || st_distance(st_geomfromtext('%s',25832), geom.geo_field) || ')' ||", punkt);         
            }
            String queryMain = "SELECT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_NAME + "') AS class_id, "
                + TABLE_NAME + "." + FIELD__ID + ", "
                + TABLE_NAME + "." + FIELD__NAME + " || ' (' || " + distance + "|| ')'"
                + " FROM " + TABLE_NAME;
            
                

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format(" LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.format(" WHERE %s", String.join(" AND ", wheres)) : "";
            final String orderBy = (!orderBys.isEmpty()) ? String.format(" ORDER BY %s", String.join(", ", orderBys)) : "";
            
            final String query = String.format("%s %s %s %s",queryMain, leftJoin, where, orderBy);
            LOG.info(query);
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
            final List<MetaObjectNode> mons = new ArrayList<>();
            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = String.valueOf(al.get(2));
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final RemoteException ex) {
            LOG.error("error while searching for SubObject", ex);
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
