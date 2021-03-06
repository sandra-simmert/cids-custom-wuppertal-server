/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.billing;

import java.io.Serializable;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class BillingModus implements Serializable {

    //~ Instance fields --------------------------------------------------------

    private String key;
    private String name;
    private Boolean discounts;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Modus object.
     */
    public BillingModus() {
    }

    /**
     * Creates a new Modus object.
     *
     * @param  key        DOCUMENT ME!
     * @param  name       DOCUMENT ME!
     * @param  discounts  DOCUMENT ME!
     */
    public BillingModus(final String key, final String name, final Boolean discounts) {
        this.key = key;
        this.name = name;
        this.discounts = discounts;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Boolean getDiscounts() {
        return discounts;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  discounts  DOCUMENT ME!
     */
    public void setDiscounts(final Boolean discounts) {
        this.discounts = discounts;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getKey() {
        return key;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  key  DOCUMENT ME!
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getName() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  name  DOCUMENT ME!
     */
    public void setName(final String name) {
        this.name = name;
    }
}
