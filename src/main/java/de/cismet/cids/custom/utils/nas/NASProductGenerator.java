/**
 * *************************************************
 *
 * cismet GmbH, Saarbruecken, Germany
 * 
* ... and it just works.
 * 
***************************************************
 */
package de.cismet.cids.custom.utils.nas;

import Sirius.server.newuser.User;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.vividsolutions.jts.geom.Geometry;

import de.aed_sicad.namespaces.svr.AMAuftragServer;
import de.aed_sicad.namespaces.svr.AuftragsManager;
import de.aed_sicad.namespaces.svr.AuftragsManagerSoap;

import org.openide.util.Exceptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.cismet.cids.custom.wunda_blau.search.actions.NasDataQueryAction;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * DOCUMENT ME!
 *
 * @author daniel
 * @version $Revision$, $Date$
 */
public class NASProductGenerator {

    //~ Static fields/initializers ---------------------------------------------
    private static final String FILE_APPENDIX = ".xml";
    private static NASProductGenerator instance;
    //~ Instance fields --------------------------------------------------------
    final File openOrdersLogFile;
    final File undeliveredOrdersLogFile;
    private final transient org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    private AuftragsManagerSoap manager;
    private final String SERVICE_URL;
    private final String USER;
    private final String PW;
    private final String OUTPUT_DIR;
    private HashMap<String, HashSet<String>> openOrderMap = new HashMap<String, HashSet<String>>();
    private HashMap<String, HashSet<String>> undeliveredOrderMap = new HashMap<String, HashSet<String>>();

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new NASProductGenerator object.
     *
     * @throws RuntimeException DOCUMENT ME!
     */
    private NASProductGenerator() {
        final Properties serviceProperties = new Properties();
        try {
            serviceProperties.load(NASProductGenerator.class.getResourceAsStream("nasServer_conf.properties"));
            SERVICE_URL = serviceProperties.getProperty("service");
            USER = serviceProperties.getProperty("user");
            PW = serviceProperties.getProperty("password");
            OUTPUT_DIR = serviceProperties.getProperty("outputDir");
            final File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                log.error("could not write to the given nas output directory " + outputDir);
                throw new RuntimeException("could not write to the given nas output directory " + outputDir);
            }
            final StringBuilder fileNameBuilder = new StringBuilder(OUTPUT_DIR);
            fileNameBuilder.append(System.getProperty("file.separator"));
            openOrdersLogFile = new File(fileNameBuilder.toString() + "openOrdersMap.json");
            undeliveredOrdersLogFile = new File(fileNameBuilder.toString() + "undeliveredOrdersMap.json");
            if (!openOrdersLogFile.exists()) {
                openOrdersLogFile.createNewFile();
            }
            if (!undeliveredOrdersLogFile.exists()) {
                undeliveredOrdersLogFile.createNewFile();
            }
            if (!(openOrdersLogFile.isFile() && openOrdersLogFile.canWrite())
                    || !(undeliveredOrdersLogFile.isFile() && undeliveredOrdersLogFile.canWrite())) {
                log.error("could not write to order log files");
            }
            initFromOrderLogFiles();
        } catch (Exception ex) {
            log.fatal("NAS Datenabgabe initialisation Error!", ex);
            throw new RuntimeException(ex);
        }
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static NASProductGenerator instance() {
        if (instance == null) {
            instance = new NASProductGenerator();
        }
        return instance;
    }

    /**
     * DOCUMENT ME!
     */
    private void initFromOrderLogFiles() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            openOrderMap = transformJsonMap(mapper.readValue(openOrdersLogFile, Map.class));
            undeliveredOrderMap = transformJsonMap(mapper.readValue(undeliveredOrdersLogFile, Map.class));
            // check of there are open orders that arent downloaded from the 3a server yet
            for (final String userId : openOrderMap.keySet()) {
                final HashSet<String> openOrderIds = openOrderMap.get(userId);
                for (final String orderId : openOrderIds) {
                    final Thread workerThread = new Thread(new NasProductDownloader(userId, orderId));
                    workerThread.start();
                }
            }
        } catch (JsonParseException ex) {
            log.error("Could not parse nas order log files", ex);
        } catch (JsonMappingException ex) {
            log.error("error while json mapping/unmarshalling of nas order log file", ex);
        } catch (IOException ex) {
            log.error("error while loading nas order log file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param loadedJsonObj DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private HashMap<String, HashSet<String>> transformJsonMap(final Map<String, ArrayList<String>> loadedJsonObj) {
        final HashMap<String, HashSet<String>> map = new HashMap<String, HashSet<String>>();
        for (final String s : loadedJsonObj.keySet()) {
            final HashSet<String> orderIdSet = new HashSet<String>(loadedJsonObj.get(s));
            map.put(s, orderIdSet);
        }
        return map;
    }

    /**
     * DOCUMENT ME!
     *
     * @param geom DOCUMENT ME!
     * @param templateFile DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private InputStream generateQeury(final Geometry geom, final InputStream templateFile) {
        int gmlId = 0;
        try {
            final String xmlGeom = GML3Writer.writeGML3_2WithETRS89(geom);
            // parse the queryTemplate and insert the geom in it
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(templateFile);
            final NodeList intersectNodes = doc.getElementsByTagName("ogc:Intersects");
            final Document doc2 = dBuilder.parse(new InputSource(new StringReader(xmlGeom)));
            final Element newPolygonNode = doc2.getDocumentElement();
            for (int i = 0; i < intersectNodes.getLength(); i++) {
                Node oldPolygonNode = null;
                Node child = intersectNodes.item(i).getFirstChild();
                while (child != null) {
                    if (child.getNodeName().equals("gml:Polygon")) {
                        oldPolygonNode = child;
                        break;
                    }
                    child = child.getNextSibling();
                }
                if (oldPolygonNode == null) {
                    log.error("corrupt query template file, could not find a geometry node");
                }
                newPolygonNode.setAttribute("gml:id", "G" + gmlId);
                gmlId++;
                final Node importedNode = doc.importNode(newPolygonNode, true);
                intersectNodes.item(i).removeChild(oldPolygonNode);
                intersectNodes.item(i).appendChild(importedNode);
            }
            // Use a Transformer for output
            final TransformerFactory tFactory = TransformerFactory.newInstance();
            final Transformer transformer = tFactory.newTransformer();

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final DOMSource source = new DOMSource(doc);
            final StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
            if (log.isDebugEnabled()) {
                log.debug(outputStream.toString());
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (ParserConfigurationException ex) {
            log.error("Parser Configuration Error", ex);
        } catch (SAXException ex) {
            log.error("Error during parsing document", ex);
        } catch (IOException ex) {
            log.error("Error while openeing nas template file", ex);
        } catch (TransformerConfigurationException ex) {
            log.error("Error writing adopted nas template file", ex);
        } catch (TransformerException ex) {
            log.error("Error writing adopted nas template file", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param template DOCUMENT ME!
     * @param geom DOCUMENT ME!
     * @param user DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String executeAsynchQuery(final NasDataQueryAction.PRODUCT_TEMPLATE template,
            final Geometry geom,
            final User user) {
        InputStream templateFile = null;

        try {
            if (template == NasDataQueryAction.PRODUCT_TEMPLATE.KOMPLETT) {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_komplett.xml");
            } else if (template == NasDataQueryAction.PRODUCT_TEMPLATE.OHNE_EIGENTUEMER) {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_o_eigentuemer.xml");
            } else {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_points.xml");
            }
        } catch (Exception ex) {
            log.fatal("ka", ex);
        }
        if (geom == null) {
            log.error("geometry is null, cannot execute nas query");
            return null;
        }

        initAmManager();
        final InputStream preparedQuery = generateQeury(geom, templateFile);
        final int sessionID = manager.login(USER, PW);
        final String orderId = manager.registerGZip(sessionID, gZipFile(preparedQuery));

        addToOpenOrders(deterimineUserPrefix(user), orderId);
        addToUndeliveredOrders(deterimineUserPrefix(user), orderId);

        final Thread workerThread = new Thread(new NasProductDownloader(deterimineUserPrefix(user), orderId));
        workerThread.start();

        return orderId;
    }

    private void initAmManager() {
        final AuftragsManager am;
        try {
            am = new AuftragsManager(new URL(SERVICE_URL));
        } catch (Exception ex) {
            log.error("error creating 3AServer interface", ex);
            return;
        }
        manager = am.getAuftragsManagerSoap();
    }

    /**
     * DOCUMENT ME!
     *
     * @param orderId DOCUMENT ME!
     * @param user DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public byte[] getResultForOrder(final String orderId, final User user) {
        final HashSet<String> openUserOrders = openOrderMap.get(deterimineUserPrefix(user));
        if ((openUserOrders != null) && openUserOrders.contains(orderId)) {
            if (log.isDebugEnabled()) {
                log.debug("requesting an order that isnt not done");
            }
            return null;
        }
        final HashSet<String> undeliveredUserOrders = undeliveredOrderMap.get(deterimineUserPrefix(user));
        if ((undeliveredUserOrders == null) || undeliveredUserOrders.isEmpty()) {
            log.error("there are no undelivered nas orders for the user " + user.toString());
            return null;
        }
        removeFromUndeliveredOrders(deterimineUserPrefix(user), orderId);
        return loadFile(deterimineUserPrefix(user), orderId);
    }

    /**
     * DOCUMENT ME!
     *
     * @param user DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Set<String> getUndeliveredOrders(final User user) {
        return undeliveredOrderMap.get(deterimineUserPrefix(user));
    }

    /**
     * DOCUMENT ME!
     *
     * @param is queryFile DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private byte[] gZipFile(final InputStream is) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream zipOut = null;
        try {
            zipOut = new GZIPOutputStream(bos);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                zipOut.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            is.close();
            zipOut.close();
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            log.error("error during gzip of gile", ex);
        } catch (IOException ex) {
            log.error("error during gzip of gile", ex);
        } finally {
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param protocol DOCUMENT ME!
     */
    private void logProtocol(final byte[] protocol) {
        final byte[] unzippedProtocol = gunzip(protocol);
        if (log.isDebugEnabled()) {
            log.debug("Nas Protokoll " + new String(unzippedProtocol));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param data DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private byte[] gunzip(final byte[] data) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new GZIPInputStream(new ByteArrayInputStream(data));
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (IOException ex) {
            log.error("error during gunzip of nas response files", ex);
        } finally {
            try {
                bos.close();
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userKey DOCUMENT ME!
     * @param orderId DOCUMENT ME!
     * @param data DOCUMENT ME!
     */
    private void unzipAndSaveFile(final String userKey, final String orderId, final byte[] data) {
        final File file = new File(determineFileName(userKey, orderId));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new GZIPInputStream(new ByteArrayInputStream(data));
            os = new FileOutputStream(file);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                os.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
        } catch (IOException ex) {
            log.error("error during gunzip of nas response files", ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param userKey DOCUMENT ME!
     * @param orderId DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private byte[] loadFile(final String userKey, final String orderId) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new FileInputStream(determineFileName(userKey, orderId));
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            log.error("could not find result file for order id " + orderId);
        } catch (IOException ex) {
            log.error("error during loading result file for order id " + orderId);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                bos.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userKey DOCUMENT ME!
     * @param orderId DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private String determineFileName(final String userKey, final String orderId) {
        final StringBuilder fileNameBuilder = new StringBuilder(OUTPUT_DIR);
        fileNameBuilder.append(System.getProperty("file.separator"));
        fileNameBuilder.append(userKey);
        fileNameBuilder.append(System.getProperty("file.separator"));
        fileNameBuilder.append(orderId);
        fileNameBuilder.append(FILE_APPENDIX);
        return fileNameBuilder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param user DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private String deterimineUserPrefix(final User user) {
        return user.getId() + "_" + user.getName();
    }

    /**
     * DOCUMENT ME!
     *
     * @param userKey userId DOCUMENT ME!
     * @param orderId DOCUMENT ME!
     */
    private void addToOpenOrders(final String userKey, final String orderId) {
        HashSet<String> openUserOders = openOrderMap.get(userKey);
        if (openUserOders == null) {
            openUserOders = new HashSet<String>();
            openOrderMap.put(userKey, openUserOders);
        }
        openUserOders.add(orderId);
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param userKey userId DOCUMENT ME!
     * @param orderId DOCUMENT ME!
     */
    private void removeFromOpenOrders(final String userKey, final String orderId) {
        final HashSet<String> openUserOrders = openOrderMap.get(userKey);
        if (openUserOrders == null) {
            log.info("there are no undelivered nas orders for the user with id " + userKey);
            return;
        }
        openUserOrders.remove(orderId);
        if (openUserOrders.isEmpty()) {
            openOrderMap.remove(userKey);
        }
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param userKey userId DOCUMENT ME!
     * @param orderId DOCUMENT ME!
     */
    private void addToUndeliveredOrders(final String userKey, final String orderId) {
        HashSet<String> undeliveredUserOders = undeliveredOrderMap.get(userKey);
        if (undeliveredUserOders == null) {
            undeliveredUserOders = new HashSet<String>();
            undeliveredOrderMap.put(userKey, undeliveredUserOders);
        }
        undeliveredUserOders.add(orderId);
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param userKey userId DOCUMENT ME!
     * @param orderId DOCUMENT ME!
     */
    private void removeFromUndeliveredOrders(final String userKey, final String orderId) {
        final HashSet<String> undeliveredUserOders = undeliveredOrderMap.get(userKey);
        if (undeliveredUserOders == null) {
            log.info("there are no undelivered nas orders for the user with id " + userKey);
            return;
        }
        undeliveredUserOders.remove(orderId);
        if (undeliveredUserOders.isEmpty()) {
            undeliveredOrderMap.remove(userKey);
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
            writer.writeValue(undeliveredOrdersLogFile, undeliveredOrderMap);
            writer.writeValue(openOrdersLogFile, openOrderMap);
        } catch (IOException ex) {
            log.error("error during writing open and undelivered order maps to file", ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    /**
     * DOCUMENT ME!
     *
     * @version $Revision$, $Date$
     */
    private class NasProductDownloader implements Runnable {

        //~ Instance fields ----------------------------------------------------
        private String orderId;
        private String userId;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new NasProductDownloader object.
         *
         * @param userId DOCUMENT ME!
         * @param orderId DOCUMENT ME!
         */
        public NasProductDownloader(final String userId, final String orderId) {
            this.orderId = orderId;
            this.userId = userId;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void run() {
            initAmManager();
            final int sessionID = manager.login(USER, PW);
            AMAuftragServer amServer = manager.listAuftrag(sessionID, orderId);
            while (amServer.getWannBeendet() == null) {
                amServer = manager.listAuftrag(sessionID, orderId);
            }

            logProtocol(manager.getProtocolGZip(sessionID, orderId));
            unzipAndSaveFile(userId, orderId, manager.getResultGZip(sessionID, orderId));

            removeFromOpenOrders(userId, orderId);
        }
    }
}