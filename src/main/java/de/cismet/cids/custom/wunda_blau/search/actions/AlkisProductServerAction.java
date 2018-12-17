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

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import java.net.URL;

import java.util.Date;

import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.alkis.ServerAlkisConf;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.exceptions.BadHttpStatusCodeException;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlkisProductServerAction implements ConnectionContextStore, UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final Logger LOG = Logger.getLogger(AlkisProductServerAction.class);
    public static final String TASK_NAME = "alkisProduct";
    private static final int MAX_BUFFER_SIZE = 1024;
    private static final String MASK_REPLACEMENT = "***";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Body {

        //~ Enum constants -----------------------------------------------------

        KARTE, KARTE_CUSTOM, EINZELNACHWEIS, EINZELNACHWEIS_STICHTAG, LISTENNACHWEIS
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        ALKIS_CODE, PRODUKT, FERTIGUNGSVERMERK, STICHTAG, X, Y, MASSSTAB, MASSSTAB_MIN, MASSSTAB_MAX, WINKEL, ZUSATZ,
        AUFTRAGSNUMMER
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            String produkt = null;
            String alkisCode = null;
            String fertigungsVermerk = null;
            Date stichtag = null;
            Integer winkel = null;
            Integer x = null;
            Integer y = null;
            String massstab = null;
            String massstabMin = null;
            String massstabMax = null;
            String zusatz = null;
            String auftragsnummer = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(AlkisProductServerAction.Parameter.PRODUKT.toString())) {
                        produkt = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.ALKIS_CODE.toString())) {
                        alkisCode = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.FERTIGUNGSVERMERK.toString())) {
                        fertigungsVermerk = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.STICHTAG.toString())) {
                        stichtag = (Date)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.WINKEL.toString())) {
                        winkel = (Integer)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.X.toString())) {
                        x = (Integer)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.Y.toString())) {
                        y = (Integer)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.MASSSTAB.toString())) {
                        massstab = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.MASSSTAB_MIN.toString())) {
                        massstabMin = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.MASSSTAB_MAX.toString())) {
                        massstabMax = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.ZUSATZ.toString())) {
                        zusatz = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.AUFTRAGSNUMMER.toString())) {
                        auftragsnummer = (String)sap.getValue();
                    }
                }
            }

            final URL url;

            switch ((Body)body) {
                case KARTE: {
                    url = ServerAlkisProducts.getInstance().productKarteUrl(alkisCode, fertigungsVermerk);
                }
                break;
                case KARTE_CUSTOM: {
                    url = ServerAlkisProducts.getInstance()
                                .productKarteUrl(
                                        alkisCode,
                                        produkt,
                                        winkel,
                                        x,
                                        y,
                                        massstab,
                                        massstabMin,
                                        massstabMax,
                                        zusatz,
                                        auftragsnummer,
                                        true,
                                        fertigungsVermerk);
                }
                break;
                case EINZELNACHWEIS: {
                    url = ServerAlkisProducts.getInstance()
                                .productEinzelNachweisUrl(
                                        alkisCode,
                                        produkt,
                                        getUser(),
                                        fertigungsVermerk);
                }
                break;
                case EINZELNACHWEIS_STICHTAG: {
                    url = ServerAlkisProducts.getInstance()
                                .productEinzelnachweisStichtagsbezogenUrl(
                                        alkisCode,
                                        produkt,
                                        stichtag,
                                        getUser());
                }
                break;
                case LISTENNACHWEIS: {
                    url = ServerAlkisProducts.getInstance().productListenNachweisUrl(alkisCode, produkt);
                }
                break;
                default: {
                    url = null;
                }
            }
            if (url != null) {
                final int parameterPosition = url.toString().indexOf('?');
                if (Body.LISTENNACHWEIS.equals(body) && (parameterPosition >= 0)) {
                    return doDownload(new URL(url.toString().substring(0, parameterPosition)),
                            new StringReader(url.toString().substring(parameterPosition + 1)));
                } else {
                    return doDownload(url, null);
                }
            } else {
                throw new Exception("url could not be generated");
            }
        } catch (final BadHttpStatusCodeException ex) {
            LOG.error(ex, ex);
            return new RuntimeException(mask(ex.getResponse()));
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return new RuntimeException(mask(ex.getMessage()));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mask  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String mask(final String mask) {
        return mask.replaceAll(ServerAlkisConf.getInstance().getCreds().getUser(), MASK_REPLACEMENT)
                    .replaceAll(ServerAlkisConf.getInstance().getCreds().getPassword(), MASK_REPLACEMENT);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   url               DOCUMENT ME!
     * @param   requestParameter  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private byte[] doDownload(final URL url, final Reader requestParameter) throws Exception {
        final InputStream src;
        if (requestParameter != null) {
            src =
                new SimpleHttpAccessHandler().doRequest(
                    url,
                    requestParameter,
                    AccessHandler.ACCESS_METHODS.POST_REQUEST,
                    AlkisProducts.POST_HEADER);
        } else {
            src = new SimpleHttpAccessHandler().doRequest(url);
        }
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        while (true) {
            final byte[] buffer = new byte[MAX_BUFFER_SIZE];
            final int read = src.read(buffer);
            if (read < 0) {
                return os.toByteArray();
            } else {
                os.write(buffer, 0, read);
            }
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
