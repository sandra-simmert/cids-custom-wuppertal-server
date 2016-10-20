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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import org.apache.commons.io.FileUtils;

import java.io.File;

import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationRequest;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenAnfrageBean;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.wunda_blau.search.actions.PointNumberReserverationServerAction;

import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskPNR extends VermessungsunterlagenTask {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "PNR";

    //~ Instance fields --------------------------------------------------------

    private final String auftragsnummer;
    private final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[] punktnummernreservierungBeans;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskRisseBilder object.
     *
     * @param  jobKey                         DOCUMENT ME!
     * @param  auftragsnummer                 DOCUMENT ME!
     * @param  punktnummernreservierungBeans  DOCUMENT ME!
     */
    public VermUntTaskPNR(final String jobKey,
            final String auftragsnummer,
            final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[] punktnummernreservierungBeans) {
        super(
            TYPE,
            jobKey);

        this.auftragsnummer = auftragsnummer;
        this.punktnummernreservierungBeans = punktnummernreservierungBeans;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    protected void performTask() throws Exception {
        if (punktnummernreservierungBeans != null) {
            for (final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean bean
                        : punktnummernreservierungBeans) {
                try {
                    final String protokoll = doReservations(bean).getProtokoll();

                    final String filename = getPath() + "/" + auftragsnummer + "_"
                                + bean.getUtmKilometerQuadrat() + ".txt";
                    FileUtils.writeStringToFile(new File(filename), protokoll);
                } catch (final Exception exception) {
                    VermessungsunterlagenHelper.writeExceptionJson(
                        exception,
                        getPath()
                                + "/fehlerprotokoll_"
                                + bean.getUtmKilometerQuadrat()
                                + ".json");
                }
            }
        }
    }

    @Override
    protected String getSubPath() {
        return "/PNR";
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    protected PointNumberReservationRequest doReservations(
            final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean bean) throws Exception {
        final boolean verlaengern = false; // TODO ?

        final ServerActionParameter sapAction;
        if (verlaengern) {
            sapAction = new ServerActionParameter(
                    PointNumberReserverationServerAction.PARAMETER_TYPE.ACTION.toString(),
                    PointNumberReserverationServerAction.ACTION_TYPE.PROLONG_RESERVATION);
        } else {
            sapAction = new ServerActionParameter(
                    PointNumberReserverationServerAction.PARAMETER_TYPE.ACTION.toString(),
                    PointNumberReserverationServerAction.ACTION_TYPE.DO_RESERVATION);
        }
        final ServerActionParameter sapZulNmr = new ServerActionParameter(
                PointNumberReserverationServerAction.PARAMETER_TYPE.PREFIX.toString(),
                bean.getKatasteramtsID().substring(2));
        final ServerActionParameter sapAuftragsnummer = new ServerActionParameter(
                PointNumberReserverationServerAction.PARAMETER_TYPE.AUFTRAG_NUMMER.toString(),
                auftragsnummer);
        final ServerActionParameter sapNummerierungsbezirk = new ServerActionParameter(
                PointNumberReserverationServerAction.PARAMETER_TYPE.NBZ.toString(),
                bean.getUtmKilometerQuadrat());
        final ServerActionParameter sapAnzahl = new ServerActionParameter(
                PointNumberReserverationServerAction.PARAMETER_TYPE.ANZAHL.toString(),
                bean.getAnzahlPunktnummern());
        final ServerActionParameter sapStartwert = new ServerActionParameter(
                PointNumberReserverationServerAction.PARAMETER_TYPE.STARTWERT.toString(),
                0);

        final PointNumberReserverationServerAction action = new PointNumberReserverationServerAction();
        final PointNumberReservationRequest request = (PointNumberReservationRequest)action.execute(
                null,
                sapAction,
                sapZulNmr,
                sapAuftragsnummer,
                sapNummerierungsbezirk,
                sapAnzahl,
                sapStartwert);
        return request;
    }
}