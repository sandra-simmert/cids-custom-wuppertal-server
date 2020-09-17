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
package de.cismet.cids.custom.tostringconverter.wunda_blau;

import de.cismet.cids.tools.CustomToStringConverter;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
public class BaumArtToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__NAME = "name";                // baum_Art
    public static final String FIELD__HAUTART = "fk_haupart.name";  // baum_Hauptart
    public static final String FIELD__ID = "id";                    // baum_Art

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final String myid = String.valueOf(cidsBean.getProperty(FIELD__ID));
        if ("-1".equals(myid)) {
            return "Neue Art anlegen";
        } else {
            String myName = String.valueOf(cidsBean.getProperty(FIELD__NAME));
            String myMain = String.valueOf(cidsBean.getProperty(FIELD__NAME));
            return myName + " - " + myMain;
        }
    }
}
