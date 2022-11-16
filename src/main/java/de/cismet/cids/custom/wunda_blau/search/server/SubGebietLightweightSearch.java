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
public class SubGebietLightweightSearch extends AbstractMonToLwmoSearch {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private SubGebietSearch monSearch;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SubGebietLightweightSearch object.
     */
    public SubGebietLightweightSearch() {
        this(new SubGebietSearch());
    }

    /**
     * Creates a new SubGebietLightweightSearch object.
     *
     * @param  monSearch  DOCUMENT ME!
     */
    public SubGebietLightweightSearch(final SubGebietSearch monSearch) {
        super(createSearchInfo(), "sub_gebiet", "WUNDA_BLAU");
        this.monSearch = monSearch;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SearchInfo createSearchInfo() {
        return new SearchInfo(SubGebietLightweightSearch.class.getName(),
                SubGebietLightweightSearch.class.getSimpleName(),
                "SubGebietLS",
                Arrays.asList(new SearchParameterInfo[0]),
                new SearchParameterInfo());
    }
}
