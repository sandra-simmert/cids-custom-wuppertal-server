/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;
import Sirius.server.sql.DBConnection;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.Collection;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class WohnlagenKategorisierungServerAction implements ServerAction, UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(WohnlagenKategorisierungServerAction.class);
    public static final String TASK_NAME = "wohnlagenKategorisierung";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        WOHNLAGEN, KATEGORIE, BEMERKUNG
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            Collection<MetaObjectNode> wohnlageNodes = null;
            MetaObjectNode kategorieNode = null;
            String bemerkung = null;
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(ParameterType.WOHNLAGEN.toString())) {
                        wohnlageNodes = (Collection)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.KATEGORIE.toString())) {
                        kategorieNode = (MetaObjectNode)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BEMERKUNG.toString())) {
                        bemerkung = (String)sap.getValue();
                    }
                }
            }

            if (wohnlageNodes != null) {
                final String DELETE_QUERY =
                    "DELETE FROM wohnlage_kategorisierung WHERE fk_wohnlage = ? AND login_name = ?";
                final String INSERT_QUERY_WITH_KATEGORIE =
                    "INSERT INTO wohnlage_kategorisierung (fk_wohnlage, login_name, bemerkung, fk_kategorie) VALUES (?, ?, ?, ?)";
                final String INSERT_QUERY_WITHOUT_KATEGORIE =
                    "INSERT INTO wohnlage_kategorisierung (fk_wohnlage, login_name, bemerkung) VALUES (?, ?, ?)";
                final PreparedStatement delete;
                final PreparedStatement insert;

                final Connection connection = DomainServerImpl.getServerInstance().getConnectionPool().getConnection();
                delete = connection.prepareStatement(DELETE_QUERY);

                if (kategorieNode == null) {
                    insert = connection.prepareStatement(INSERT_QUERY_WITHOUT_KATEGORIE);
                } else {
                    insert = connection.prepareStatement(INSERT_QUERY_WITH_KATEGORIE);
                }
                try {
                    connection.setAutoCommit(false);
                    for (final MetaObjectNode wohnlageNode : wohnlageNodes) {
                        delete.setInt(1, wohnlageNode.getObjectId());
                        delete.setString(2, getUser().getName());
                        delete.executeUpdate();

                        insert.setInt(1, wohnlageNode.getObjectId());
                        insert.setString(2, getUser().getName());
                        insert.setString(3, bemerkung);
                        if (kategorieNode != null) {
                            insert.setInt(4, kategorieNode.getObjectId());
                        }
                        insert.executeUpdate();
                    }
                    connection.commit();
                } catch (final SQLException ex) {
                    LOG.error("Error while updating wohnlage_kategorisierung. rolling back", ex);
                    connection.rollback();
                } finally {
                    if (delete != null) {
                        delete.close();
                    }
                    if (insert != null) {
                        insert.close();
                    }
                    DBConnection.closeConnections(connection);
                }
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }

        return null;
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
        return this.metaService;
    }
}
