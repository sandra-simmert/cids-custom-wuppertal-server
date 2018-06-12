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
package de.cismet.cids.custom.utils.alkis;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.awt.Point;

import java.io.StringReader;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.cismet.cids.dynamics.CidsBean;

import static de.cismet.cids.custom.utils.alkis.AlkisPunktReportScriptlet.SUFFIXES;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class AlkisProducts {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AlkisProducts.class);

    public static final String HEADER_CONTENTTYPE_KEY = "Content-Type";
    public static final String HEADER_CONTENTTYPE_VALUE_POST = "application/x-www-form-urlencoded";
    public static final HashMap<String, String> POST_HEADER = new HashMap<String, String>();

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Type {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKSNACHWEIS_PDF, FLURSTUECKSNACHWEIS_HTML, FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF,
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML, FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF,
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML, FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF,
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML,

        BESTANDSNACHWEIS_NRW_PDF, BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF, BESTANDSNACHWEIS_NRW_HTML,
        BESTANDSNACHWEIS_KOMMUNAL_PDF, BESTANDSNACHWEIS_KOMMUNAL_HTML, BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF,
        BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML, GRUNDSTUECKSNACHWEIS_NRW_PDF, GRUNDSTUECKSNACHWEIS_NRW_HTML,

        PUNKTLISTE_PDF, PUNKTLISTE_HTML, PUNKTLISTE_TXT
    }

    //~ Instance fields --------------------------------------------------------

    private final Map<String, Point> alkisFormats;
    private final List<AlkisProductDescription> alkisMapProducts;
    private final String nachverarbeitungScript;

    private final AlkisConf alkisConf;
    //
    private final Map<Type, String> productMap = new HashMap();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisProducts object.
     *
     * @param   alkisConf               DOCUMENT ME!
     * @param   productProperties       DOCUMENT ME!
     * @param   formatProperties        DOCUMENT ME!
     * @param   produktbeschreibungXml  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public AlkisProducts(final AlkisConf alkisConf,
            final Properties productProperties,
            final Properties formatProperties,
            final String produktbeschreibungXml) throws Exception {
        this.alkisConf = alkisConf;

        POST_HEADER.put(HEADER_CONTENTTYPE_KEY, HEADER_CONTENTTYPE_VALUE_POST);

        final List<AlkisProductDescription> mapProducts = new ArrayList<>();
        final Map<String, Point> formatMap = new HashMap<>();
        alkisFormats = Collections.unmodifiableMap(formatMap);
        alkisMapProducts = Collections.unmodifiableList(mapProducts);
        nachverarbeitungScript = productProperties.getProperty("NACHVERARBEITUNG_SCRIPT");
        productMap.put(Type.FLURSTUECKSNACHWEIS_PDF, productProperties.getProperty("FLURSTUECKSNACHWEIS_PDF"));
        productMap.put(Type.FLURSTUECKSNACHWEIS_HTML, productProperties.getProperty("FLURSTUECKSNACHWEIS_HTML"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML"));

        productMap.put(
            Type.GRUNDSTUECKSNACHWEIS_NRW_PDF,
            productProperties.getProperty("GRUNDSTUECKSNACHWEIS_NRW_PDF"));
        productMap.put(
            Type.GRUNDSTUECKSNACHWEIS_NRW_HTML,
            productProperties.getProperty("GRUNDSTUECKSNACHWEIS_NRW_HTML"));
        productMap.put(Type.BESTANDSNACHWEIS_NRW_PDF, productProperties.getProperty("BESTANDSNACHWEIS_NRW_PDF"));
        productMap.put(
            Type.BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF,
            productProperties.getProperty("BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF"));
        productMap.put(Type.BESTANDSNACHWEIS_NRW_HTML, productProperties.getProperty("BESTANDSNACHWEIS_NRW_HTML"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_PDF,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_PDF"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_HTML,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_HTML"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML"));

        productMap.put(Type.PUNKTLISTE_PDF, productProperties.getProperty("PUNKTLISTE_PDF"));
        productMap.put(Type.PUNKTLISTE_HTML, productProperties.getProperty("PUNKTLISTE_HTML"));
        productMap.put(Type.PUNKTLISTE_TXT, productProperties.getProperty("PUNKTLISTE_TXT"));

        final Document document = new SAXBuilder().build(new StringReader(produktbeschreibungXml));
        // ---------Kartenprodukte----------
        for (final Object o0 : document.getRootElement().getChildren()) {
            final Element category = (Element)o0;
            final String catName = category.getName();
            if ("Karte".equals(catName)) {
                for (final Object o1 : category.getChildren()) {
                    final Element productClass = (Element)o1;
                    if (productClass.getName().matches(".*[Kk]lasse.*")) {
                        final String clazz = productClass.getAttribute("Name").getValue();
                        for (final Object o2 : productClass.getChildren()) {
                            final Element guiProduct = (Element)o2;
                            final String type = guiProduct.getAttribute("ProduktnameAuswertung").getValue();
                            final Attribute defaultProductAttr = guiProduct.getAttribute(
                                    "defaultProduct");
                            boolean defaultProduct;
                            if (defaultProductAttr != null) {
                                defaultProduct = defaultProductAttr.getBooleanValue();
                            } else {
                                defaultProduct = false;
                            }
                            final Attribute productDefaultScaleAttr = guiProduct.getAttribute(
                                    "productDefaultScale");
                            Integer productDefaultScale;
                            if (productDefaultScaleAttr != null) {
                                productDefaultScale = productDefaultScaleAttr.getIntValue();
                            } else {
                                productDefaultScale = null;
                            }
                            for (final Object o3 : guiProduct.getChildren()) {
                                final Element singleProduct = (Element)o3;
                                final Attribute codeAttr = singleProduct.getAttribute("ID");
                                if (codeAttr != null) {
                                    final String code = codeAttr.getValue();
                                    final String dinFormatCode = singleProduct.getAttribute("Layout").getValue();
                                    final String layoutDim = formatProperties.getProperty(dinFormatCode);
                                    int width = -1;
                                    int height = -1;
                                    if (layoutDim == null) {
                                        org.apache.log4j.Logger.getLogger(ServerAlkisProducts.class)
                                                .info("Can not find format dimensions for: " + dinFormatCode);
                                    } else {
                                        final String[] dims = layoutDim.split("(x|X)");
                                        width = Integer.parseInt(dims[0]);
                                        height = Integer.parseInt(dims[1]);
                                        formatMap.put(dinFormatCode, new Point(width, height));
                                    }

                                    // Preisfaktoren
                                    final Element preisFaktoren = (Element)singleProduct.getChildren().get(0);
                                    final String dinFormat = preisFaktoren.getAttribute("DINFormat").getValue();
                                    final String fileFormat = preisFaktoren.getAttribute("Dateiformat").getValue();
                                    final Attribute massstabAttr = preisFaktoren.getAttribute("Massstab");
                                    String massstab;
                                    if (massstabAttr != null) {
                                        massstab = preisFaktoren.getAttribute("Massstab").getValue();
                                    } else {
                                        massstab = "-";
                                    }
                                    final Attribute massstabMinAttr = preisFaktoren.getAttribute("MassstabMin");
                                    String massstabMin = null;
                                    if (massstabMinAttr != null) {
                                        massstabMin = preisFaktoren.getAttribute("MassstabMin").getValue();
                                    }
                                    final Attribute massstabMaxAttr = preisFaktoren.getAttribute("MassstabMin");
                                    String massstabMax = null;
                                    if (massstabMaxAttr != null) {
                                        massstabMax = preisFaktoren.getAttribute("MassstabMax").getValue();
                                    }

                                    // Stempelfeld
                                    StempelfeldInfo stempelfeldInfo = null;
                                    final Element stempelFeldInfoElement = (Element)singleProduct.getChild(
                                            "Stempelfeld",
                                            singleProduct.getNamespace());
                                    if (stempelFeldInfoElement != null) {
                                        final float fromX = stempelFeldInfoElement.getAttribute("fromX")
                                                    .getFloatValue();
                                        final float fromY = stempelFeldInfoElement.getAttribute("fromY")
                                                    .getFloatValue();
                                        final float toX = stempelFeldInfoElement.getAttribute("toX").getFloatValue();
                                        final float toY = stempelFeldInfoElement.getAttribute("toY").getFloatValue();
                                        stempelfeldInfo = new StempelfeldInfo(fromX, fromY, toX, toY);
                                    }

                                    final AlkisProductDescription currentProduct = new AlkisProductDescription(
                                            clazz,
                                            type,
                                            code,
                                            dinFormat,
                                            massstab,
                                            massstabMin,
                                            massstabMax,
                                            fileFormat,
                                            width,
                                            height,
                                            defaultProduct,
                                            stempelfeldInfo,
                                            productDefaultScale);
                                    mapProducts.add(currentProduct);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Map<String, Point> getAlkisFormats() {
        return alkisFormats;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<AlkisProductDescription> getAlkisMapProducts() {
        return alkisMapProducts;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getNachverarbeitungScript() {
        return nachverarbeitungScript;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String get(final Type type) {
        return productMap.get(type);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String getPointDataForProduct(final CidsBean pointBean) {
        final StringBuilder sb = new StringBuilder("AX_");
        sb.append(pointBean.getProperty("pointtype"));
        sb.append(":");
        sb.append(pointBean.getProperty("pointcode"));
        return sb.toString().replace(" ", "");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointcode  dgkBlattnummer the value of dgkBlattnummer
     *
     * @return  DOCUMENT ME!
     */
    public Collection<URL> getCorrespondingPointURLs(final String pointcode) {
        final Collection<URL> validURLs = new LinkedList<>();

        // The pointcode of a alkis point has a specific format:
        // 25xx56xx1xxxxx
        // ^  ^
        // |  Part 2 of the "Kilometerquadrat"
        // Part 1 of the "Kilometerquadrat"
        if ((pointcode == null) || (pointcode.trim().length() < 9) || (pointcode.trim().length() > 15)) {
            return validURLs;
        }

        final StringBuilder urlBuilder;
        if (pointcode.trim().length() < 15) {
            urlBuilder = new StringBuilder(alkisConf.getApmapsHost());

            final String kilometerquadratPart1 = pointcode.substring(2, 4);
            final String kilometerquadratPart2 = pointcode.substring(6, 8);

            urlBuilder.append('/');
            urlBuilder.append(kilometerquadratPart1);
            urlBuilder.append(kilometerquadratPart2);
            urlBuilder.append('/');
            urlBuilder.append(alkisConf.getApmapsPrefix());
            urlBuilder.append(pointcode);
            urlBuilder.append('.');
        } else {
            urlBuilder = new StringBuilder(alkisConf.getApmapsEtrsHost());
            urlBuilder.append('/');
            urlBuilder.append(alkisConf.getApmapsPrefix());
            urlBuilder.append(pointcode);
            urlBuilder.append('.');
        }
        for (final String suffix : SUFFIXES) {
            URL urlToTry = null;
            try {
                urlToTry = new URL(urlBuilder.toString() + suffix);
            } catch (MalformedURLException ex) {
                LOG.warn("The URL '" + urlBuilder.toString() + suffix
                            + "' is malformed. Can't load the corresponding picture.",
                    ex);
            }

            if (urlToTry != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Valid URL: " + urlToTry.toExternalForm());
                }

                validURLs.add(urlToTry);
            }
        }
        return validURLs;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   dgkBlattnummer  the value of dgkBlattnummer
     * @param   laufendeNummer  the value of laufendeNummer
     *
     * @return  DOCUMENT ME!
     */
    public Collection<URL> getCorrespondingNivPURLs(final java.lang.String dgkBlattnummer,
            final String laufendeNummer) {
        final Collection<URL> validURLs = new LinkedList<URL>();
        final StringBuilder urlBuilder = new StringBuilder(alkisConf.getNivpHost());
        urlBuilder.append('/');
        urlBuilder.append(dgkBlattnummer);
        urlBuilder.append('/');
        urlBuilder.append(alkisConf.getNivpPrefix());
        urlBuilder.append(dgkBlattnummer);
        urlBuilder.append(getFormattedLaufendeNummerNivP(laufendeNummer));
        urlBuilder.append('.');
        for (final String suffix : SUFFIXES) {
            URL urlToTry = null;
            try {
                urlToTry = new URL(urlBuilder.toString() + suffix);
            } catch (MalformedURLException ex) {
                LOG.warn("The URL '" + urlBuilder.toString() + suffix
                            + "' is malformed. Can't load the corresponding picture.",
                    ex);
            }

            if (urlToTry != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Valid URL: " + urlToTry.toExternalForm());
                }

                validURLs.add(urlToTry);
            }
        }
        return validURLs;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   laufendeNummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String getFormattedLaufendeNummerNivP(final String laufendeNummer) {
        final StringBuilder result;

        if (laufendeNummer == null) {
            result = new StringBuilder("000");
        } else {
            result = new StringBuilder(laufendeNummer);
        }

        while (result.length() < 3) {
            result.insert(0, "0");
        }

        return result.toString();
    }
}
