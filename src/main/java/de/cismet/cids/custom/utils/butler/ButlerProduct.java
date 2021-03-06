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

import java.io.Serializable;

import java.util.ArrayList;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class ButlerProduct implements Serializable {

    //~ Instance fields --------------------------------------------------------

    String key;
    String name;
    int colorDepth;
    ButlerResolution resolution;
    ButlerFormat format;
    String volumeParamText;
    String scale;
    ArrayList<ButlerResolution> butlerResolutions;

    //~ Methods ----------------------------------------------------------------

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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getColorDepth() {
        return colorDepth;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  colorDepth  DOCUMENT ME!
     */
    public void setColorDepth(final int colorDepth) {
        this.colorDepth = colorDepth;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ButlerResolution getResolution() {
        return resolution;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  resolution  DOCUMENT ME!
     */
    public void setResolution(final ButlerResolution resolution) {
        this.resolution = resolution;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ButlerFormat getFormat() {
        return format;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  outputFormat  DOCUMENT ME!
     */
    public void setFormat(final ButlerFormat outputFormat) {
        this.format = outputFormat;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getVolumeParamText() {
        return volumeParamText;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  volumeParamText  DOCUMENT ME!
     */
    public void setVolumeParamText(final String volumeParamText) {
        this.volumeParamText = volumeParamText;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getScale() {
        return scale;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  scale  DOCUMENT ME!
     */
    public void setScale(final String scale) {
        this.scale = scale;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<ButlerResolution> getButlerResolutions() {
        return butlerResolutions;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  butlerResolutions  DOCUMENT ME!
     */
    public void setButlerResolutions(final ArrayList<ButlerResolution> butlerResolutions) {
        this.butlerResolutions = butlerResolutions;
    }
}
