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
package de.cismet.cids.custom.utils.vermessungsunterlagen;

import lombok.Getter;

import org.apache.log4j.Logger;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class VermessungsunterlagenProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenProperties.class);
    public static final String FROM_WEBDAV = "webdav";
    public static final String FROM_FTP = "ftp";

    //~ Instance fields --------------------------------------------------------

    private final Properties properties;
    private final String cidsLogin;
    private final String absPathTmp;
    private final boolean ftpEnabled;
    private final String ftpHost;
    private final String ftpLogin;
    private final String ftpPass;
    private final String ftpPath;
    private final boolean webDavEnabled;
    private final String webDavHost;
    private final String webDavLogin;
    private final String webDavPass;
    private final String webDavPath;
    private final String absPathTest;
    private final String absPathPdfRisse;
    private final String absPathPdfNivP;
    private final String absPathPdfPnrVermstelle;
    private final String jobResultFrom;
    private final String downloadFrom;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    public VermessungsunterlagenProperties(final Properties properties) {
        this.properties = properties;

        cidsLogin = readProperty("CIDS_LOGIN", null);
        absPathTmp = readProperty("ABS_PATH_TMP", "/tmp");
        ftpEnabled = Boolean.parseBoolean(readProperty("FTP_ENABLED", "true"));
        ftpHost = readProperty("FTP_HOST", null);
        ftpLogin = readProperty("FTP_LOGIN", null);
        ftpPass = readProperty("FTP_PASS", null);
        ftpPath = readProperty("FTP_PATH", null);
        webDavEnabled = Boolean.parseBoolean(readProperty("WEBDAV_ENABLED", "false"));
        webDavHost = readProperty("WEBDAV_HOST", null);
        webDavLogin = readProperty("WEBDAV_LOGIN", null);
        webDavPass = readProperty("WEBDAV_PASS", null);
        webDavPath = readProperty("WEBDAV_PATH", null);
        absPathTest = readProperty("ABS_PATH_TEST", null);
        absPathPdfRisse = readProperty("ABS_PATH_PDF_RISSE", null);
        absPathPdfNivP = readProperty("ABS_PATH_PDF_NIVP", null);
        absPathPdfPnrVermstelle = readProperty("ABS_PATH_PDF_PNR_VERMSTELLE", null);
        jobResultFrom = readProperty("JOB_RESULT_FROM", "webdav");
        downloadFrom = readProperty("DOWNLOAD_FROM", "webdav");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   property      DOCUMENT ME!
     * @param   defaultValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String readProperty(final String property, final String defaultValue) {
        String value = defaultValue;
        try {
            value = getProperties().getProperty(property, defaultValue);
        } catch (final Exception ex) {
            final String message = "could not read " + property + " from "
                        + WundaBlauServerResources.VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES.getValue()
                        + ". setting to default value: " + defaultValue;
            LOG.warn(message, ex);
        }
        return value;
    }
}
