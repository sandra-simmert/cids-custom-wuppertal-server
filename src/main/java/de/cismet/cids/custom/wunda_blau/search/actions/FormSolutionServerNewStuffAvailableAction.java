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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionBestellungSpecialLogger;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsBestellungHandler;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionServerNewStuffAvailableAction implements UserAwareServerAction,
    MetaServiceStore,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionServerNewStuffAvailableAction.class);

    public static final String TASK_NAME = "formSolutionServerNewStuffAvailable";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        STEP_TO_EXECUTE,
        @Deprecated
        SINGLE_STEP, REPAIR_ERRORS, METAOBJECTNODES, TEST
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;
    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        FormSolutionBestellungSpecialLogger.getInstance().log("execute by: " + getUser().getName());

        boolean repairErrors = false;
        int startStep = FormSolutionsBestellungHandler.STATUS_FETCH;
        boolean test = false;
        Collection<MetaObjectNode> mons = null;
        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(PARAMETER_TYPE.METAOBJECTNODES.toString())) {
                    mons = new ArrayList<>();
                    mons.addAll((Collection)sap.getValue());
                } else if (sap.getKey().equals(PARAMETER_TYPE.STEP_TO_EXECUTE.toString())) {
                    startStep = (Integer)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.REPAIR_ERRORS.toString())) {
                    repairErrors = (Boolean)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.TEST.toString())) {
                    test = (Boolean)sap.getValue();
                }
            }
        }

        final FormSolutionsBestellungHandler handler = new FormSolutionsBestellungHandler(
                getMetaService(),
                getConnectionContext());
        if (mons == null) {
            return handler.fetchEndExecuteAllOpen(test);
        } else {
            return handler.execute(startStep, repairErrors, test, mons);
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

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
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
