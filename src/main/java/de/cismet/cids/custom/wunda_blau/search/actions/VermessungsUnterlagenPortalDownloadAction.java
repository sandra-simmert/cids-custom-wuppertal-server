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
package de.cismet.cids.custom.wunda_blau.search.actions;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenProperties;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VermessungsUnterlagenPortalDownloadAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "VUPDownloadAction";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String schluessel = (String)body;
        InputStream inputStream = null;
        try {
            final String downloadFrom = VermessungsunterlagenHelper.getInstance().getProperties().getDownloadFrom();
            if (VermessungsunterlagenProperties.FROM_WEBDAV.equals(downloadFrom)) {
                final String tmp = VermessungsunterlagenHelper.getInstance().getProperties().getWebDavPath();
                final String webDavPath = (tmp.isEmpty() ? "" : ("/" + tmp)) + "/"
                            + VermessungsunterlagenHelper.DIR_PREFIX
                            + "_" + schluessel + ".zip";
                inputStream = VermessungsunterlagenHelper.getInstance().downloadFromWebDAV(webDavPath);
            } else if (VermessungsunterlagenProperties.FROM_FTP.equals(downloadFrom)) {
                final String tmp = VermessungsunterlagenHelper.getInstance().getProperties().getFtpPath();
                final String ftpZipPath = (tmp.isEmpty() ? "" : ("/" + tmp)) + "/"
                            + VermessungsunterlagenHelper.DIR_PREFIX
                            + "_" + schluessel + ".zip";
                inputStream = VermessungsunterlagenHelper.getInstance().downloadFromFTP(ftpZipPath);
            }
            return IOUtils.toByteArray(inputStream);
        } catch (final Exception ex) {
            return new Exception("Fehler beim Herunterladen der Zip-Datei.", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
