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
public class PoiLocationinstanceGeomSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(PoiLocationinstanceGeomSearch.class);

       public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String QUERY_TEMPLATE_WE = 
            "SELECT (SELECT id from cs_class WHERE name ilike 'Point_of_Interest') AS classid, id, \n" +
                "CASE WHEN distance IS NOT NULL THEN name || ' [' || distance::int || 'm]' ELSE name END AS name, \n" +
                "distance \n" + 
            "FROM ";
    private static final String QUERY_TEMPLATE =
            "SELECT (SELECT id from cs_class WHERE name ilike 'Point_of_Interest') AS classid, id, name, 99999999 AS distance \n" +
            "FROM (\n" +
                "SELECT poi_locationinstance.id AS id, \n" +
                    "poi_locationinstance.geographicidentifier AS name\n" +
                "FROM poi_locationinstance\n" +
                "ORDER BY poi_locationinstance.geographicidentifier\n" +
            ") AS ohneGeom \n" +
            "ORDER BY distance, name";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter private Integer limitAnz;
    @Setter @Getter private Geometry geom;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumMeldungSearch object.
     */
    public PoiLocationinstanceGeomSearch() {
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
            String query;
            if (getGeom() != null){
                final int srid = getGeom().getSRID();
                final List<String> selectsWE = new ArrayList<>();
                    //selectsWE.add("(SELECT id from cs_class WHERE name ilike 'Point_of_Interest') AS classid");
                    selectsWE.add("poi_locationinstance.id AS id");
                    selectsWE.add("poi_locationinstance.geographicidentifier AS name");
                    //selectsWE.add(String.format("st_distance(geom.geo_field, GeomFromEWKT(%d)) AS distance", getGeom()));
                    selectsWE.add("st_distance(geom.geo_field, GeomFromEWKT('SRID=" + srid + ";" + getGeom() + "')) AS distance");
                final String selectWE = String.format("SELECT %s", String.join(", ", selectsWE));
                final String fromWE = " FROM poi_locationinstance";
                final String leftJoinWE = " LEFT JOIN geom ON geom.id = poi_locationinstance.pos";
                final String orderByWE = " ORDER BY distance ASC, poi_locationinstance.geographicidentifier";
                final String limitWE = String.format(" LIMIT %d", getLimitAnz());
                final String queryWE = String.format("%s %s %s %s %s", selectWE, fromWE, leftJoinWE, orderByWE, limitWE);
            
                final String queryWEouter = String.format("%s (%s) AS withGeom ", QUERY_TEMPLATE_WE, queryWE);
            LOG.info(queryWEouter);
                final String union = " UNION ";
                query = String.format("%s %s %s", queryWEouter, union, QUERY_TEMPLATE);
            } else {
                query = QUERY_TEMPLATE;
            }
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
            LOG.error("error while searching for poi with geom", ex);
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
