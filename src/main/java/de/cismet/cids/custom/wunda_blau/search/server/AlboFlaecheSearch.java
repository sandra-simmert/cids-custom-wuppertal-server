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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenException;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class AlboFlaecheSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    StorableSearch<AlboFlaecheSearch.FlaecheSearchInfo> {

    //~ Static fields/initializers ---------------------------------------------

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(ArtInfo.class, new ArtInfoDeserializer());
        OBJECT_MAPPER.registerModule(module);
    }

    private static final transient Logger LOG = Logger.getLogger(AlboFlaecheSearch.class);
    private static final String QUERY_TEMPLATE = "SELECT DISTINCT ON (flaeche.erhebungsnummer) "
                + "(SELECT c.id FROM cs_class c WHERE table_name ILIKE 'albo_flaeche') AS class_id, flaeche.id, flaeche.erhebungsnummer || ' [' || art.schluessel || ']' AS name "
                + "FROM albo_flaeche AS flaeche "
                + "LEFT JOIN albo_flaechenart AS art ON flaeche.fk_art = art.id "
                + "%s "
                + "WHERE %s "
                + "ORDER BY flaeche.erhebungsnummer;";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchMode {

        //~ Enum constants -----------------------------------------------------

        AND, OR,
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private FlaecheSearchInfo searchInfo;
    @Getter @Setter private Geometry geometry;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlboFlaecheSearch object.
     */
    public AlboFlaecheSearch() {
        this(new FlaecheSearchInfo());
    }

    /**
     * Creates a new AlboFlaecheSearch object.
     *
     * @param  searchInfo  DOCUMENT ME!
     */
    public AlboFlaecheSearch(final FlaecheSearchInfo searchInfo) {
        this.searchInfo = searchInfo;
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
     */
    public AlboFlaecheLightweightSearch getLightweightSearch() {
        return new AlboFlaecheLightweightSearch(this);
    }

    @Override
    public void setSearchInfo(final FlaecheSearchInfo searchInfo) {
        this.searchInfo = searchInfo;
    }

    @Override
    public void setSearchInfo(final Object searchInfo) {
        if (searchInfo instanceof FlaecheSearchInfo) {
            this.searchInfo = (FlaecheSearchInfo)searchInfo;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @Override
    public String createQuery() {
        final FlaecheSearchInfo searchInfo = getSearchInfo();
        if (searchInfo != null) {
            final String buffer = SearchProperties.getInstance().getIntersectsBuffer();
            final List<String> leftJoins = new ArrayList<>();
            final Collection<String> wheres = new ArrayList<>();
            final Collection<String> wheresMain = new ArrayList<>();
            final Collection<String> wheresArt = new ArrayList<>();

            leftJoins.add("albo_vorgang_flaeche AS arr ON flaeche.id = arr.fk_flaeche");

            if ((searchInfo.getErhebungsNummer() != null) && !searchInfo.getErhebungsNummer().isEmpty()) {
                wheresMain.add(String.format(
                        "flaeche.erhebungsnummer ILIKE '%%%s%%'",
                        searchInfo.getErhebungsNummer()));
            }

            if ((searchInfo.getVorgangSchluessel() != null) && !searchInfo.getVorgangSchluessel().isEmpty()) {
                leftJoins.add("albo_vorgang AS vorgang ON vorgang.arr_flaechen = arr.vorgang_reference");
                wheresMain.add(String.format("vorgang.schluessel LIKE '%%%s%%'", searchInfo.getVorgangSchluessel()));
            }

            if ((searchInfo.getStatusSchluessel() != null) && !searchInfo.getStatusSchluessel().isEmpty()) {
                leftJoins.add("albo_flaechenstatus AS status ON status.id = flaeche.fk_status");
                wheresMain.add(String.format("status.schluessel LIKE '%s'", searchInfo.getStatusSchluessel()));
            }

            if ((searchInfo.getTypSchluessel() != null) && !searchInfo.getTypSchluessel().isEmpty()) {
                leftJoins.add("albo_flaechentyp AS typ ON typ.id = flaeche.fk_typ");
                wheresMain.add(String.format("typ.schluessel LIKE '%s'", searchInfo.getTypSchluessel()));
            }

            if ((searchInfo.getZuordnungSchluessel() != null) && !searchInfo.getZuordnungSchluessel().isEmpty()) {
                leftJoins.add("albo_flaechenzuordnung AS zuordnung ON zuordnung.id = flaeche.fk_zuordnung");
                wheresMain.add(String.format("zuordnung.schluessel LIKE '%s'", searchInfo.getZuordnungSchluessel()));
            }

            if (searchInfo.getLoeschen() != null) {
                wheresMain.add(String.format("flaeche.loeschen = %s", searchInfo.getLoeschen() ? "TRUE" : "FALSE"));
            }

            if (searchInfo.getArtInfos() != null) {
                int artCount = 0;
                for (final ArtInfo artInfo : searchInfo.getArtInfos()) {
                    final String artSchluessel = (artInfo != null) ? artInfo.getFlaechenartSchluessel() : null;
                    final Collection<String> subAndWheres = new ArrayList<>();
                    if (artSchluessel != null) {
                        final String alias = String.format("%03d", artCount++);
                        leftJoins.add(String.format(
                                "albo_flaechenart AS art%1$s ON art%1$s.id = flaeche.fk_art",
                                alias));
                        subAndWheres.add(String.format("art%s.schluessel LIKE '%s'", alias, artSchluessel));
                        if (artInfo instanceof StandortInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String wirtschaftszweig = ((StandortInfo)artInfo).getWzSchluessel();
                            if (wirtschaftszweig != null) {
                                subLeftJoins.add(String.format(
                                        "albo_standort_wirtschaftszweig AS stwz%1$s ON stwz%1$s.standort_reference = standort%1$s.id",
                                        alias));
                                subLeftJoins.add(String.format(
                                        "albo_wirtschaftszweig AS wz%1$s ON stwz%1$s.fk_wirtschaftszweig = wz%1$s.id",
                                        alias));
                                subAndWheres.add(String.format("wz%s.schluessel LIKE '%s'", alias, wirtschaftszweig));
                            }

                            if (!subLeftJoins.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_standort AS standort%1$s ON standort%1$s.fk_flaeche = flaeche.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        } else if (artInfo instanceof AltablagerungInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String stilllegung = ((AltablagerungInfo)artInfo).getStilllegungSchluessel();
                            final String erhebungsklasse = ((AltablagerungInfo)artInfo).getErhebungsklasseSchluessel();
                            final String verfuellkategorie = ((AltablagerungInfo)artInfo)
                                        .getVerfuellkategorieSchluessel();
                            if (stilllegung != null) {
                                subLeftJoins.add(String.format(
                                        "albo_stilllegung AS stilllegung%1$s ON altablagerung%1$s.fk_stilllegung = stilllegung%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "stilllegung%s.schluessel LIKE '%s'",
                                        alias,
                                        stilllegung));
                            }
                            if (erhebungsklasse != null) {
                                subLeftJoins.add(String.format(
                                        "albo_erhebungsklasse AS erhebungsklasse%1$s ON altablagerung%1$s.fk_erhebungsklasse = erhebungsklasse%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "erhebungsklasse%s.schluessel LIKE '%s'",
                                        alias,
                                        erhebungsklasse));
                            }
                            if (verfuellkategorie != null) {
                                subLeftJoins.add(String.format(
                                        "albo_verfuellkategorie AS verfuellkategorie%1$s ON altablagerung%1$s.fk_verfuellkategorie = verfuellkategorie%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "verfuellkategorie%s.schluessel LIKE '%s'",
                                        alias,
                                        verfuellkategorie));
                            }
                            if (!subLeftJoins.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_altablagerung AS altablagerung%1$s ON flaeche.fk_altablagerung = altablagerung%1$s.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        } else if (artInfo instanceof BewirtschaftungsschadenInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String bewirtschaftungsschadensart = ((BewirtschaftungsschadenInfo)artInfo)
                                        .getBewirtschaftungsschadensartSchluessel();
                            if (bewirtschaftungsschadensart != null) {
                                subLeftJoins.add(String.format(
                                        "albo_bewirtschaftungsschadensart AS bewirtschaftungsschadensart%1$s ON bewirtschaftungsschaden%1$s.fk_art = bewirtschaftungsschadensart%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "bewirtschaftungsschaden%s.schluessel LIKE '%s'",
                                        alias,
                                        bewirtschaftungsschadensart));
                            }
                            if (!subLeftJoins.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_bewirtschaftungsschaden AS bewirtschaftungsschaden%1$s ON flaeche.fk_bewirtschaftungsschaden = bewirtschaftungsschaden%1$s.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        } else if (artInfo instanceof SchadensfallInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String schadensfallart = ((SchadensfallInfo)artInfo).getSchadensfallartSchluessel();
                            if (schadensfallart != null) {
                                subLeftJoins.add(String.format(
                                        "albo_schadensfallart AS schadensfallart%1$s ON schadensfall%1$s.fk_art = schadensfallart%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "schadensfall%s.schluessel LIKE '%s'",
                                        alias,
                                        schadensfallart));
                            }
                            if (!subLeftJoins.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_schadensfall AS schadensfall%1$s ON flaeche.fk_schadensfall = schadensfall%1$s.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        } else if (artInfo instanceof MaterialaufbringungInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String materialaufbringungsart = ((MaterialaufbringungInfo)artInfo)
                                        .getMaterialaufbringungsartSchluessel();
                            if (materialaufbringungsart != null) {
                                subLeftJoins.add(String.format(
                                        "albo_materialaufbringungsart AS materialaufbringungsart%1$s ON materialaufbringung%1$s.fk_art = materialaufbringungsart%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "materialaufbringung%s.schluessel LIKE '%s'",
                                        alias,
                                        materialaufbringungsart));
                            }
                            if (!subLeftJoins.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_materialaufbringung AS materialaufbringung%1$s ON flaeche.fk_materialaufbringung = materialaufbringung%1$s.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        } else if (artInfo instanceof ImmissionInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String immissionsart = ((ImmissionInfo)artInfo).getImmissionsartSchluessel();
                            if (immissionsart != null) {
                                subLeftJoins.add(String.format(
                                        "albo_immissionsart AS immissionsart%1$s ON immission%1$s.fk_art = immissionsart%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "immission%s.schluessel LIKE '%s'",
                                        alias,
                                        immissionsart));
                            }
                            if (!subLeftJoins.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_immission AS immission%1$s ON flaeche.fk_immission = immission%1$s.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        }
                    }
                    if (!subAndWheres.isEmpty()) {
                        wheresArt.add(String.format("(%s)", String.join(" AND ", subAndWheres)));
                    }
                }
            }

            if (getGeometry() != null) {
                final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(getGeometry());
                leftJoins.add("geom ON flaeche.fk_geom = geom.id");
                wheres.add(String.format("geom.geo_field && GeometryFromText('%s')", geomString));
                wheres.add(String.format(
                        "intersects(st_buffer(geo_field, %s), GeometryFromText('%s'))",
                        buffer,
                        geomString));
            }

            if (!wheresMain.isEmpty()) {
                switch (searchInfo.getSearchModeMain()) {
                    case AND: {
                        wheres.add(String.format("(%s)", String.join(" AND ", wheresMain)));
                    }
                    break;
                    case OR: {
                        wheres.add(String.format("(%s)", String.join(" OR ", wheresMain)));
                    }
                }
            }

            if (!wheresArt.isEmpty()) {
                switch (searchInfo.getSearchModeArt()) {
                    case AND: {
                        wheres.add(String.format("(%s)", String.join(" AND ", wheresArt)));
                    }
                    break;
                    case OR: {
                        wheres.add(String.format("(%s)", String.join(" OR ", wheresArt)));
                    }
                }
            }

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.join(" AND ", wheres) : "TRUE";
            final String query = String.format(QUERY_TEMPLATE, leftJoin, where);
            return query;
        } else {
            return null;
        }
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final String query = createQuery();

            final List<MetaObjectNode> mons = new ArrayList<>();
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = String.valueOf(al.get(2));
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final Exception ex) {
            LOG.error("error while searching for albo_flaeche", ex);
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

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class FlaecheSearchInfo extends StorableSearch.Info {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String vorgangSchluessel;
        @JsonProperty private String erhebungsNummer;
        @JsonProperty private String statusSchluessel;
        @JsonProperty private String typSchluessel;
        @JsonProperty private String zuordnungSchluessel;
        @JsonProperty private Boolean loeschen;
        @JsonProperty private SearchMode searchModeMain;
        @JsonProperty private SearchMode searchModeArt;
        @JsonProperty private Collection<ArtInfo> artInfos;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public abstract static class ArtInfo extends StorableSearch.Info {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private final String flaechenartSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new ArtInfo object.
         *
         * @param  flaechenartSchluessel  DOCUMENT ME!
         */
        protected ArtInfo(final String flaechenartSchluessel) {
            this.flaechenartSchluessel = flaechenartSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public abstract static class StandortInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String wzSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new StandortInfo object.
         *
         * @param  artSchluessel  DOCUMENT ME!
         */
        public StandortInfo(final String artSchluessel) {
            super(artSchluessel);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class AltstandortInfo extends StandortInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new AltstandortInfo object.
         */
        public AltstandortInfo() {
            super("altstandort");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class OhneVerdachtInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new OhneVerdachtInfo object.
         */
        public OhneVerdachtInfo() {
            super("ohne_verdacht");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class BewirtschaftungsschadenInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String bewirtschaftungsschadensartSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new BewirtschaftungsschadenInfo object.
         */
        public BewirtschaftungsschadenInfo() {
            this(null);
        }

        /**
         * Creates a new BewirtschaftungsschadenInfo object.
         *
         * @param  bewirtschaftungsschadensartSchluessel  DOCUMENT ME!
         */
        public BewirtschaftungsschadenInfo(final String bewirtschaftungsschadensartSchluessel) {
            super("bewirtschaftungsschaden");
            this.bewirtschaftungsschadensartSchluessel = bewirtschaftungsschadensartSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class MaterialaufbringungInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String materialaufbringungsartSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MaterialaufbringungInfo object.
         */
        public MaterialaufbringungInfo() {
            this(null);
        }

        /**
         * Creates a new MaterialaufbringungInfo object.
         *
         * @param  materialaufbringungsartSchluessel  DOCUMENT ME!
         */
        public MaterialaufbringungInfo(final String materialaufbringungsartSchluessel) {
            super("materialaufbringung");
            this.materialaufbringungsartSchluessel = materialaufbringungsartSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class ImmissionInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String immissionsartSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new ImmissionInfo object.
         */
        public ImmissionInfo() {
            this(null);
        }

        /**
         * Creates a new ImmissionInfo object.
         *
         * @param  immissionsartSchluessel  DOCUMENT ME!
         */
        public ImmissionInfo(final String immissionsartSchluessel) {
            super("immission");
            this.immissionsartSchluessel = immissionsartSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class SchadensfallInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String schadensfallartSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SchadensfallInfo object.
         */
        public SchadensfallInfo() {
            this(null);
        }

        /**
         * Creates a new SchadensfallInfo object.
         *
         * @param  schadensfallartSchluessel  DOCUMENT ME!
         */
        public SchadensfallInfo(final String schadensfallartSchluessel) {
            super("schadensfall");
            this.schadensfallartSchluessel = schadensfallartSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class AltablagerungInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String stilllegungSchluessel;
        @JsonProperty private String verfuellkategorieSchluessel;
        @JsonProperty private String erhebungsklasseSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new AltablagerungInfo object.
         */
        public AltablagerungInfo() {
            super("altablagerung");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class BetriebsstandortInfo extends StandortInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new BetrienbsstandortInfo object.
         */
        public BetriebsstandortInfo() {
            super("betriebsstandort");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class ArtInfoDeserializer extends StdDeserializer<ArtInfo> {

        //~ Instance fields ----------------------------------------------------

        private final ObjectMapper defaultMapper = new ObjectMapper();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new CidsBeanJsonDeserializer object.
         */
        public ArtInfoDeserializer() {
            super(VermessungsunterlagenException.class);

            defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public ArtInfo deserialize(final JsonParser jp, final DeserializationContext dc) throws IOException,
            JsonProcessingException {
            final ObjectNode on = jp.readValueAsTree();
            if (on.has("flaechenartSchluessel")) {
                final String flaechenartSchluessel = (String)defaultMapper.treeToValue(on.get("flaechenartSchluessel"),
                        String.class);
                switch (flaechenartSchluessel) {
                    case "betriebsstandort": {
                        return defaultMapper.treeToValue(on, BetriebsstandortInfo.class);
                    }
                    case "altstandort": {
                        return defaultMapper.treeToValue(on, AltstandortInfo.class);
                    }
                    case "altablagerung": {
                        return defaultMapper.treeToValue(on, AltablagerungInfo.class);
                    }
                    case "immission": {
                        return defaultMapper.treeToValue(on, ImmissionInfo.class);
                    }
                    case "materialaufbringung": {
                        return defaultMapper.treeToValue(on, MaterialaufbringungInfo.class);
                    }
                    case "bewirtschaftungsschaden": {
                        return defaultMapper.treeToValue(on, BewirtschaftungsschadenInfo.class);
                    }
                    case "schadensfall": {
                        return defaultMapper.treeToValue(on, SchadensfallInfo.class);
                    }
                    case "ohne_verdacht": {
                        return defaultMapper.treeToValue(on, OhneVerdachtInfo.class);
                    }
                }
            }
            throw new RuntimeException("invalid ArtInfo");
        }
    }
}
