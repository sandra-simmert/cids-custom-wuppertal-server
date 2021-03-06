/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils.butler;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.newuser.User;
import Sirius.server.property.ServerProperties;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Properties;

import javax.ws.rs.core.MediaType;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.nas.CidsActionClient;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.cidsx.server.api.types.ActionTask;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class ButlerProductGenerator {

    //~ Static fields/initializers ---------------------------------------------

    private static ButlerProductGenerator instance;
    private static final Logger LOG = Logger.getLogger(ButlerProductGenerator.class);
    private static final String FILE_APPENDIX = ".but";
    private static final String SEPERATOR = ";";
    private static final String EASTING = "$RECHTSWERT$";
    private static final String NORTHING = "$HOCHWERT$";
    private static final String BOX_SIZE = "$VORLAGE$";
    private static final String RESOLUTION = "$AUFLOESUNG$";
    private static final String FORMAT = "$AUSGABEFORMAT$";
    private static final String FILE_NAME = "$DATEINAME$";
    private static final String LAYER = "$LAYER$";
    private static final String ETRS89_LAYER = "39";
    private static final String GK_LAYER = "36";
    private static final String MAP_SCALE = "$SCALE$";
    private static final String PDF_OPTIONS_VAR = "$PDF_OPTIONS$";
    private static final String PDF_OPTIONS = "mode=\"ACTIVEVIEW\" showpdflayers=\"false\"";

    //~ Instance fields --------------------------------------------------------

    File openOrdersLogFile;
    private String ACTION_SERVICE;
    private String ACTION_DOMAIN;
    private String ACTION_USER;
    private String ACTION_PASSWORD;
    // Map that lists all open Orders to a user id
    private HashMap<Integer, HashMap<String, ButlerRequestInfo>> openOrderMap =
        new HashMap<Integer, HashMap<String, ButlerRequestInfo>>();
    private String requestFolder;
    private String butlerBasePath;
    private String BUTLER_TEMPLATES_RES_PATH = "/butler/";
    private boolean initError = false;
    private WmpsCidsAction wmpsActionClient;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ButlerProductGenerator object.
     */
    private ButlerProductGenerator() {
        try {
            final ServerProperties serverProps = DomainServerImpl.getServerProperties();
            final String resPath = serverProps.getServerResourcesBasePath();
            BUTLER_TEMPLATES_RES_PATH = resPath + BUTLER_TEMPLATES_RES_PATH;
            final Properties butlerProperties = new Properties();
            butlerProperties.load(ServerResourcesLoader.getInstance().loadStringReader(
                    WundaBlauServerResources.BUTLER_PROPERTIES.getValue()));
            butlerBasePath = butlerProperties.getProperty("butlerBasePath");
            requestFolder = butlerBasePath + System.getProperty("file.separator")
                        + butlerProperties.getProperty("butler1RequestPath");
            ACTION_DOMAIN = butlerProperties.getProperty("actionDomain");
            ACTION_SERVICE = butlerProperties.getProperty("actionServiceURL");
            ACTION_USER = butlerProperties.getProperty("actionServiceUser");
            ACTION_PASSWORD = butlerProperties.getProperty("actionServicePassword");
            if ((ACTION_DOMAIN == null) || (ACTION_SERVICE == null) || (ACTION_SERVICE == null)
                        || (ACTION_PASSWORD == null)) {
                LOG.warn(
                    "Butler initialisation Error. Can not read properties for connecting to WMPS Action");
                initError = true;
            }
            wmpsActionClient = new WmpsCidsAction(ACTION_DOMAIN, ACTION_SERVICE);
            wmpsActionClient.setBasicAuthentication(ACTION_USER, ACTION_PASSWORD);

            final StringBuilder fileNameBuilder = new StringBuilder(butlerBasePath);
            fileNameBuilder.append(System.getProperty("file.separator"));
            openOrdersLogFile = new File(fileNameBuilder.toString() + "openOrders.json");
            if (!openOrdersLogFile.exists()) {
                openOrdersLogFile.createNewFile();
                // serialiaze en empty map to the file to avoid parsing exception
                updateJsonLogFiles();
            }
            if (!(openOrdersLogFile.isFile() && openOrdersLogFile.canWrite())) {
                LOG.warn("Can not write to Butler open order log file (" + openOrdersLogFile.getPath()
                            + "). This might cause problems in Wunda_Blau Butler functionality");
                initError = true;
            }
            loadOpenOrdersFromJsonFile();
        } catch (Exception ex) {
            LOG.warn(
                "Could not load butler properties. This might cause problems in Wunda_Blau Butler functionality",
                ex);
            initError = true;
        }
        if (!initError) {
            if (!checkFolders()) {
                initError = true;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ButlerProductGenerator getInstance() {
        if (instance == null) {
            instance = new ButlerProductGenerator();
        }
        return instance;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderNumber  DOCUMENT ME!
     * @param   user         that sends the request
     * @param   product      of the product that shall be generated
     * @param   minX         lower x coordinate of the rectangle the product is generated for
     * @param   minY         lower y coordinate of the rectangle the product is generated for
     * @param   maxX         upper x coordinate of the rectangle the product is generated for
     * @param   maxY         lower y coordinate of the rectangle the product is generated for
     * @param   isGeoTiff    default no, set only to true if <code>format.equals("tif")</code> and the output file
     *                       should end with *.geotiff instead of *.tif
     *
     * @return  the requestId necessary to retrive results with
     *          {@link #getResultForRequest(java.lang.String, java.lang.String)} method
     */
    public String createButlerRequest(final String orderNumber,
            final User user,
            final ButlerProduct product,
            final double minX,
            final double minY,
            final double maxX,
            final double maxY,
            final boolean isGeoTiff) {
        if (!initError) {
            File reqeustFile = null;
            FileWriter fw = null;
            final String filename = determineRequestFileName(user, orderNumber);
            addToOpenOrderMap(user, filename, orderNumber, product);
            try {
                reqeustFile = new File(requestFolder + System.getProperty("file.separator") + filename
                                + FILE_APPENDIX);
                if (reqeustFile.exists()) {
                    // should not happen;
                    LOG.error("butler 1 request file already exists");
                    return null;
                }
                fw = new FileWriter(reqeustFile);
                final BufferedWriter bw = new BufferedWriter(fw);
                bw.write(getRequestLine(
                        product.getKey(),
                        minX,
                        minY,
                        maxX,
                        maxY,
                        product.getColorDepth(),
                        product.getResolution().getKey(),
                        isGeoTiff,
                        product.getFormat().getKey()));
                bw.close();
                return filename;
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            } finally {
                try {
                    if (fw != null) {
                        fw.close();
                    }
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderNumber        DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   product            DOCUMENT ME!
     * @param   isEtrsBlattscnitt  DOCUMENT ME!
     * @param   boxSize            DOCUMENT ME!
     * @param   middleE            DOCUMENT ME!
     * @param   middleN            DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createButler2Request(final String orderNumber,
            final User user,
            final ButlerProduct product,
            final boolean isEtrsBlattscnitt,
            final String boxSize,
            final double middleE,
            final double middleN) {
        if (!initError || (product == null)) {
            /*
             * 1. load the respective template 2. replace the values in the template 3. save the file in the right
             * folder
             */
            BufferedWriter bw = null;
            final String filename = determineRequestFileName(user, orderNumber);
            final String request = getButler2RequestLine(
                    product,
                    isEtrsBlattscnitt,
                    middleE,
                    middleN,
                    boxSize,
                    filename);
            if (request == null) {
                LOG.error("The generated Butler 2 reqeust is null.");
                return null;
            }

            try {
                final File requestFile = File.createTempFile(filename, "xml");
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(requestFile), "ISO-8859-1"));
                bw.write(request);
                bw.close();

                final ActionTask at = wmpsActionClient.createWmpsActionTask(filename, requestFile);
                if (at != null) {
                    addToOpenOrderMap(user, filename, orderNumber, product);
                    return filename;
                }
            } catch (IOException ex) {
                LOG.error("Could not write the request file for alkis datenausgabe", ex);
            } catch (Exception ex) {
                LOG.error("Could not create WMPS action, Alkis Datenausgabe equest is aborted", ex);
            } finally {
                try {
                    bw.close();
                } catch (IOException ex) {
                    LOG.error("", ex);
                }
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public HashMap<String, ButlerRequestInfo> getAllOpenUserRequests(final User user) {
        if (initError) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ButlerPrdocutGenerator doesnt work hence there was an error during the initialisation.");
            }
            return null;
        }
        if (openOrderMap.keySet().contains(user.getId())) {
            final HashMap<String, ButlerRequestInfo> result = new HashMap<String, ButlerRequestInfo>();
            result.putAll(openOrderMap.get(user.getId()));
            return result;
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean checkFolders() {
        final File requestDir = new File(requestFolder);
        if (!requestDir.exists()) {
            requestDir.mkdirs();
        }
        if (!requestDir.isDirectory() || !requestDir.canWrite()) {
            LOG.error("could not write to the given butler request directory " + requestDir);
            return false;
        }
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user       DOCUMENT ME!
     * @param   requestId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineRequestFileName(final User user, final String requestId) {
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        String filename = user.getName() + "_" + requestId + "_" + cal.get(GregorianCalendar.HOUR_OF_DAY)
                    + "_" + cal.get(GregorianCalendar.MINUTE)
                    + "_" + cal.get(GregorianCalendar.SECOND);
        filename = filename.replaceAll("Ö", "oe");
        filename = filename.replaceAll("ö", "oe");
        filename = filename.replaceAll("Ä", "ae");
        filename = filename.replaceAll("ä", "ae");
        filename = filename.replaceAll("Ü", "ue");
        filename = filename.replaceAll("ü", "ue");
        filename = filename.replaceAll("ß", "ss");
        return filename;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productId   DOCUMENT ME!
     * @param   minX        DOCUMENT ME!
     * @param   minY        DOCUMENT ME!
     * @param   maxX        DOCUMENT ME!
     * @param   maxY        DOCUMENT ME!
     * @param   colorDepth  DOCUMENT ME!
     * @param   resolution  DOCUMENT ME!
     * @param   geoTiff     DOCUMENT ME!
     * @param   format      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getRequestLine(final String productId,
            final double minX,
            final double minY,
            final double maxX,
            final double maxY,
            final int colorDepth,
            final String resolution,
            final boolean geoTiff,
            final String format) {
        //
        final StringBuffer buffer = new StringBuffer();
        // product id
        buffer.append(productId);
        buffer.append(SEPERATOR);

        // coordinates
        buffer.append(minX);
        buffer.append(SEPERATOR);
        buffer.append(minY);
        buffer.append(SEPERATOR);
        buffer.append(maxX);
        buffer.append(SEPERATOR);
        buffer.append(maxY);
        buffer.append(SEPERATOR);

        // colordepth
        buffer.append(colorDepth);
        buffer.append(SEPERATOR);

        // resolution
        double res = 0;
        if (resolution.equals("ohne")) {
            buffer.append("0");
        } else {
            final Double d = Double.parseDouble(resolution);
            res = d / 100;
//            buffer.append("0.");
            buffer.append(res);
        }
        buffer.append(SEPERATOR);

        // geotif
        if (geoTiff) {
            buffer.append("yes");
        } else {
            buffer.append("no");
        }
        buffer.append(SEPERATOR);

        // format
        buffer.append(format);
        return buffer.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product             DOCUMENT ME!
     * @param   isEtrsBlattschnitt  DOCUMENT ME!
     * @param   x                   DOCUMENT ME!
     * @param   y                   DOCUMENT ME!
     * @param   box_size            DOCUMENT ME!
     * @param   filename            DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getButler2RequestLine(final ButlerProduct product,
            final boolean isEtrsBlattschnitt,
            final double x,
            final double y,
            final String box_size,
            final String filename) {
        final String productKey = product.getKey();
        String template = loadTemplate(productKey);
        /* Karte fuer Feldvergleich. We need to check if we need to
         * use the GK-Layer or the ETRS89-layer The inserted LayerId prevents the display of the layer
         */
        if (productKey.startsWith("0903")) {
            if (isEtrsBlattschnitt) {
                template = template.replace(LAYER, GK_LAYER);
            } else {
                template = template.replace(LAYER, ETRS89_LAYER);
            }
            template = template.replace(MAP_SCALE, product.getScale());
        }
        template = template.replace(EASTING, "" + x);
        template = template.replace(NORTHING, "" + y);
        template = template.replace(BOX_SIZE, "" + box_size);
        template = template.replace(RESOLUTION, product.getResolution().getKey());
        if ((product.getFormat() == null) || (product.getFormat().getKey() == null)) {
            LOG.error("No Format for Butler Product is specified. can not create a correct request file");
            return null;
        }
        if (product.getFormat().getKey().equals("pdf")) {
            template = template.replace(PDF_OPTIONS_VAR, PDF_OPTIONS);
        } else if (product.getFormat().getKey().equals("tif")) {
            template = template.replace(PDF_OPTIONS_VAR, "");
        }
        template = template.replace(FORMAT, product.getFormat().getKey());
        template = template.replace(FILE_NAME, filename);

        return template;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productKey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String loadTemplate(final String productKey) {
        try {
            final StringBuffer templateBuffer = new StringBuffer();
            final BufferedReader fr = new BufferedReader(new InputStreamReader(
                        new FileInputStream(BUTLER_TEMPLATES_RES_PATH + "template_" + productKey + ".xml"),
                        "ISO-8859-1"));
            final char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = fr.read(buf)) != -1) {
                final String readData = String.valueOf(buf, 0, numRead);
                templateBuffer.append(readData);
            }
            fr.close();
            return templateBuffer.toString();
        } catch (FileNotFoundException ex) {
            LOG.error("Could not access Butler tempalte file: " + BUTLER_TEMPLATES_RES_PATH + "template_" + productKey
                        + ".xml",
                ex);
        } catch (IOException ex) {
            LOG.error("Could not access Butler tempalte file: " + BUTLER_TEMPLATES_RES_PATH + "template_" + productKey
                        + ".xml",
                ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user         DOCUMENT ME!
     * @param  requestId    filename DOCUMENT ME!
     * @param  userOrderId  DOCUMENT ME!
     * @param  product      DOCUMENT ME!
     */
    private void addToOpenOrderMap(final User user,
            final String requestId,
            final String userOrderId,
            final ButlerProduct product) {
        HashMap<String, ButlerRequestInfo> openUserOrders = (HashMap<String, ButlerRequestInfo>)openOrderMap.get(
                user.getId());
        if (openUserOrders == null) {
            openUserOrders = new HashMap<String, ButlerRequestInfo>();
            openOrderMap.put(user.getId(), openUserOrders);
        }
        openUserOrders.put(requestId, new ButlerRequestInfo(userOrderId, product));
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user       DOCUMENT ME!
     * @param  requestId  DOCUMENT ME!
     */
    private void removeFromOpenOrders(final User user, final String requestId) {
        final HashMap<String, ButlerRequestInfo> openUserOrders = (HashMap<String, ButlerRequestInfo>)openOrderMap.get(
                user.getId());
        if (openUserOrders != null) {
            openUserOrders.remove(requestId);
            if (openUserOrders.isEmpty()) {
                openOrderMap.remove(user.getId());
            }
        }
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     */
    private void updateJsonLogFiles() {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        try {
            final HashMap<Integer, OpenOrderMapWrapper> mapToSerialize = new HashMap<Integer, OpenOrderMapWrapper>();
            for (final Integer i : openOrderMap.keySet()) {
                final OpenOrderMapWrapper openuserOders = new OpenOrderMapWrapper(openOrderMap.get(i));
                mapToSerialize.put(i, openuserOders);
            }
            writer.writeValue(openOrdersLogFile, mapToSerialize);
        } catch (IOException ex) {
            LOG.error("error during writing open butler orders to log file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void loadOpenOrdersFromJsonFile() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final HashMap<Integer, OpenOrderMapWrapper> wrapperMap = mapper.readValue(
                    openOrdersLogFile,
                    new TypeReference<HashMap<Integer, OpenOrderMapWrapper>>() {
                    });

            for (final Integer i : wrapperMap.keySet()) {
                openOrderMap.put(i, wrapperMap.get(i).getMap());
            }
        } catch (JsonParseException ex) {
            LOG.error("Could not parse nas order log files", ex);
        } catch (JsonMappingException ex) {
            LOG.error("error while json mapping/unmarshalling of nas order log file", ex);
        } catch (IOException ex) {
            LOG.error("error while loading nas order log file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user       DOCUMENT ME!
     * @param  requestId  DOCUMENT ME!
     */
    public void removeOrder(final User user, final String requestId) {
        if (initError) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ButlerPrdocutGenerator doesnt work hence there was an error during the initialisation.");
            }
            return;
        }
        removeFromOpenOrders(user, requestId);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class OpenOrderMapWrapper {

        //~ Instance fields ----------------------------------------------------

        private HashMap<String, ButlerRequestInfo> map;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new OpenOrderMapWrapper object.
         */
        public OpenOrderMapWrapper() {
        }

        /**
         * Creates a new OpenOrderMapWrapper object.
         *
         * @param  map  DOCUMENT ME!
         */
        public OpenOrderMapWrapper(final HashMap<String, ButlerRequestInfo> map) {
            this.map = map;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public HashMap<String, ButlerRequestInfo> getMap() {
            return map;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  map  DOCUMENT ME!
         */
        public void setMap(final HashMap<String, ButlerRequestInfo> map) {
            this.map = map;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private final class WmpsCidsAction extends CidsActionClient {

        //~ Static fields/initializers -----------------------------------------

        private static final String ACTION_KEY = "wmps";

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new WmpsCidsAction object.
         *
         * @param  domain   DOCUMENT ME!
         * @param  baseURL  DOCUMENT ME!
         */
        public WmpsCidsAction(final String domain, final String baseURL) {
            super(domain, baseURL);
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param   filename     params DOCUMENT ME!
         * @param   requestFile  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public ActionTask createWmpsActionTask(final String filename,
                final File requestFile) {
            final ActionTask task = new ActionTask();
            final HashMap<String, Object> params = new HashMap<String, Object>();
            task.setParameters(params);
            return super.createTask(ACTION_KEY, task, requestFile, MediaType.APPLICATION_XML_TYPE, true);
        }
    }
}
