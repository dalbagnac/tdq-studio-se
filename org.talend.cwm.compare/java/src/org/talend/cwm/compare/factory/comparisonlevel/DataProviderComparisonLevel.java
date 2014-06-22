// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.cwm.compare.factory.comparisonlevel;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeRightTarget;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.commons.emf.FactoriesUtil;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.database.conn.DatabaseConnStrUtil;
import org.talend.core.model.general.Project;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.cwm.compare.DQStructureComparer;
import org.talend.cwm.compare.exception.ReloadCompareException;
import org.talend.cwm.db.connection.ConnectionUtils;
import org.talend.cwm.helper.CatalogHelper;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dq.helper.PropertyHelper;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.helper.resourcehelper.PrvResourceFileHelper;
import org.talend.dq.writer.EMFSharedResources;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.RepositoryNode;
import orgomg.cwm.objectmodel.core.Package;
import orgomg.cwm.resource.relational.Catalog;
import orgomg.cwm.resource.relational.Schema;

/**
 * DOC rli class global comment. Detailled comment
 */
public class DataProviderComparisonLevel extends AbstractComparisonLevel {

    private static Logger log = Logger.getLogger(DataProviderComparisonLevel.class);

    public DataProviderComparisonLevel(Object selectedObj) {
        super(selectedObj);
    }

    protected boolean isValid() {
        return selectedObj instanceof Connection || ((IFile) selectedObj).getFileExtension().equalsIgnoreCase(FactoriesUtil.PROV);
    }

    @Override
    protected Connection findDataProvider() {
        // MOD qiongli 2011-9-5 feature TDQ-3317.
        Connection provider = null;
        if (selectedObj instanceof RepositoryNode) {
            Item connItem = ((RepositoryNode) selectedObj).getObject().getProperty().getItem();
            provider = ((ConnectionItem) connItem).getConnection();
        } else if (selectedObj instanceof Connection) {
            provider = (Connection) selectedObj;
        } else {
            provider = PrvResourceFileHelper.getInstance().findProvider((IFile) selectedObj);
        }
        return provider;
    }

    @Override
    protected void saveReloadResult() {

        // MOD qiongli 2011-9-9,feature TDQ-3317
        Item item = null;
        if (selectedObj instanceof Connection) {
            Connection con = (Connection) selectedObj;
            Property property = PropertyHelper.getProperty(con);
            if (property != null) {
                item = property.getItem();
                // reSet the neweast url value for context mode,this url in item is used by tdq
                if (con.isContextMode()) {
                    if (item != null && con instanceof DatabaseConnection) {
                        DatabaseConnection dbConn = (DatabaseConnection) ConnectionUtils
                                .getOriginalDatabaseConnection((DatabaseConnection) con);
                        String urlStr = DatabaseConnStrUtil.getURLString(dbConn);
                        if (urlStr != null) {
                            ((DatabaseConnection) con).setURL(urlStr);
                            ConnectionHelper.setUsingURL(con, urlStr);
                        }

                    }
                }
            }

        } else if (selectedObj instanceof IRepositoryViewObject) {
            item = ((IRepositoryViewObject) selectedObj).getProperty().getItem();

        } else {
            // MOD klliu 2001-03-01 bug 17506
            RepositoryNode recursiveFind = RepositoryNodeHelper.recursiveFind(oldDataProvider);
            if (recursiveFind != null) {
                item = recursiveFind.getObject().getProperty().getItem();
            }
        }
        if (item == null) {
            return;
        }// ~

        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        // MOD mzhao bug 20523, only save the reloaded connection resource, the client dependency (analysis) should not
        // save here, it will be handled later.
        try {
            ProxyRepositoryFactory.getInstance().save(currentProject, item);
        } catch (PersistenceException e) {
            log.error(e, e);
        }
        // ElementWriterFactory.getInstance().createDataProviderWriter().save(item);
        // ElementWriterFactory.getInstance().createDataProviderWriter().save(oldDataProvider);
        // ProxyRepositoryViewObject.save(oldDataProvider);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.cwm.compare.factory.comparisonlevel.AbstractComparisonLevel #getSavedReloadObject()
     */
    protected EObject getSavedReloadObject() throws ReloadCompareException {
        return this.tempReloadProvider;
    }

    @Override
    protected Resource getLeftResource() throws ReloadCompareException {
        DQStructureComparer.clearSubNode(copyedDataProvider);
        List<Package> packages = new ArrayList<Package>();
        packages.addAll(ConnectionHelper.getCatalogs(copyedDataProvider));
        packages.addAll(ConnectionHelper.getSchema(copyedDataProvider));
        // URI uri =
        // URI.createPlatformResourceURI(copyedFile.getFullPath().toString(),
        // false);
        Resource leftResource = null;
        leftResource = copyedDataProvider.eResource();
        // if (tdCatalogs.size() != 0) {
        // leftResource =
        // EMFSharedResources.getInstance().getResource(copyedDataProvider
        // .eResource().getURI(),
        // true);
        // if (leftResource == null) {
        // throw new
        // ReloadCompareException("No factory has been found for URI: " +
        // copyedDataProvider.eResource().getURI());
        // }
        leftResource.getContents().clear();
        for (Package catalog : packages) {
            catalog.getDataManager().clear();
            leftResource.getContents().add(catalog);
        }
        // }
        EMFSharedResources.getInstance().saveResource(leftResource);
        return upperCaseResource(leftResource);
    }

    @Override
    protected Resource getRightResource() throws ReloadCompareException {
        List<Package> packages = new ArrayList<Package>();
        packages.addAll(ConnectionHelper.getCatalogs(tempReloadProvider));
        packages.addAll(ConnectionHelper.getSchema(tempReloadProvider));
        // URI uri = tempReloadProvider.eResource().getURI();
        Resource reloadResource = null;
        reloadResource = tempReloadProvider.eResource();
        // if (tdCatalogs.size() != 0) {
        // reloadResource = EMFSharedResources.getInstance().getResource(uri,
        // true);
        // if (reloadResource == null) {
        // throw new
        // ReloadCompareException("No factory has been found for URI: " + uri);
        // }
        reloadResource.getContents().clear();
        for (Package catalog : packages) {
            catalog.getDataManager().clear();
            reloadResource.getContents().add(catalog);
        }
        // }
        EMFSharedResources.getInstance().saveResource(reloadResource);
        return upperCaseResource(reloadResource);
    }

    @Override
    protected void handleAddElement(ModelElementChangeRightTarget addElement) {
        EObject rightElement = addElement.getRightElement();
        Catalog catalog = SwitchHelpers.CATALOG_SWITCH.doSwitch(rightElement);
        if (catalog != null) {
            // ADD xqliu 2012-05-03 TDQ-4853
            catalog.getDataManager().clear();
            // ~ TDQ-4853
            ConnectionHelper.addCatalog(catalog, oldDataProvider);
            this.tempReloadProvider.getDataPackage().remove(catalog);
        } else {
            Schema schema = SwitchHelpers.SCHEMA_SWITCH.doSwitch(rightElement);
            if (schema != null) {
                // MOD qiongli 2012-2-2 TDQ 4498,if this connection have Catalog and schema,should add schema into
                // catalog.
                EObject eContainer = schema.eContainer();
                if (eContainer != null && eContainer instanceof Catalog) {
                    Catalog schemaParent = (Catalog) eContainer;
                    List<Schema> schemas = new ArrayList<Schema>();
                    schemas.add(schema);
                    Catalog oldCatalog = CatalogHelper.getCatalog(oldDataProvider, schemaParent.getName());
                    if (oldCatalog != null) {
                        CatalogHelper.addSchemas(schemas, oldCatalog);
                        schemaParent.getOwnedElement().remove(schema);
                    }
                } else {
                    // ADD xqliu 2012-05-03 TDQ-4853
                    schema.getDataManager().clear();
                    // ~ TDQ-4853
                    ConnectionHelper.addSchema(schema, oldDataProvider);
                    this.tempReloadProvider.getDataPackage().remove(catalog);
                }
            }
        }
        return;
    }

    @Override
    protected void handleRemoveElement(ModelElementChangeLeftTarget removeElement) {
        Package removePackage = packageSwitch.doSwitch(removeElement.getLeftElement());
        if (removePackage == null) {
            return;
        }
        popRemoveElementConfirm();
        // MOD qiongli 2012-2-2 TDQ 4498,if this connection have Catalog and Schema,should remove schema from catalog.
        Schema schema = SwitchHelpers.SCHEMA_SWITCH.doSwitch(removePackage);
        if (schema != null && schema.eContainer() != null && schema.eContainer() instanceof Catalog) {
            Catalog schemaParent = (Catalog) schema.eContainer();
            Catalog oldCatalog = CatalogHelper.getCatalog(oldDataProvider, schemaParent.getName());
            if (oldCatalog != null && oldCatalog.getName().equalsIgnoreCase(schemaParent.getName())) {
                oldCatalog.getOwnedElement().remove(schema);
                if (oldCatalog.eResource() != null) {
                    oldCatalog.eResource().getContents().remove(schema);
                }
            }
            return;
        }
        oldDataProvider.getDataPackage().remove(removePackage);
        oldDataProvider.eResource().getContents().remove(removePackage);

    }

}