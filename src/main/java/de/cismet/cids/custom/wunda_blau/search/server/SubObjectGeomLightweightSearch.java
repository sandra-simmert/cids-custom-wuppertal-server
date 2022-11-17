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
package de.cismet.cids.custom.wunda_blau.search.server;

import lombok.Getter;
import lombok.Setter;

import org.openide.util.lookup.ServiceProvider;

import java.util.Arrays;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class SubObjectGeomLightweightSearch extends AbstractMonToLwmoSearch {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter public SubObjectGeomSearch monSearch;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SubObjectGeomLightweightSearch object.
     */
    public SubObjectGeomLightweightSearch() {
        this(new SubObjectGeomSearch());
    }

    /**
     * Creates a new SubObjectGeomLightweightSearch object.
     *
     * @param  monSearch  DOCUMENT ME!
     */
    public SubObjectGeomLightweightSearch(final SubObjectGeomSearch monSearch) {
        super(createSearchInfo(), "sub_object", "WUNDA_BLAU");
        this.monSearch = monSearch;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SearchInfo createSearchInfo() {
        return new SearchInfo(SubObjectGeomLightweightSearch.class.getName(),
                SubObjectGeomLightweightSearch.class.getSimpleName(),
                "SubObjectLS",
                Arrays.asList(new SearchParameterInfo[0]),
                new SearchParameterInfo());
    }
}
