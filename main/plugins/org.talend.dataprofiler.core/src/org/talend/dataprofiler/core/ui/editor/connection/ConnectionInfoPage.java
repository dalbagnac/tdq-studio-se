// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.editor.connection;

import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.database.JavaSqlFactory;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.cwm.db.connection.ConnectionUtils;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.dialog.message.DeleteModelElementConfirmDialog;
import org.talend.dataprofiler.core.ui.editor.AbstractMetadataFormPage;
import org.talend.dataprofiler.core.ui.utils.MessageUI;
import org.talend.dataquality.exception.DataprofilerCoreException;
import org.talend.dq.helper.PropertyHelper;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.nodes.ConnectionRepNode;
import org.talend.dq.nodes.DBConnectionRepNode;
import org.talend.dq.writer.impl.ElementWriterFactory;
import org.talend.metadata.managment.utils.MetadataConnectionUtils;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.wizards.metadata.connection.database.DatabaseWizard;
import org.talend.utils.sugars.ReturnCode;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * DOC rli class global comment. Detailled comment
 */
public class ConnectionInfoPage extends AbstractMetadataFormPage {

    private static Logger log = Logger.getLogger(ConnectionInfoPage.class);

    protected ConnectionRepNode connectionRepNode;

    public ConnectionRepNode getConnectionRepNode() {
        return this.connectionRepNode;
    }

    private void initConnectionRepNode(Connection conn) {
        RepositoryNode recursiveFind = RepositoryNodeHelper.recursiveFind(conn);
        if (recursiveFind != null && recursiveFind instanceof ConnectionRepNode) {
            this.connectionRepNode = (ConnectionRepNode) recursiveFind;
        }
    }

    private Text loginText;

    private Text passwordText;

    private Text urlText;

    private Button editButton;

    private Section infomatioinSection = null;

    private boolean isUrlChanged = false;

    private boolean isPassWordChanged = false;

    private boolean isLoginChanged = false;

    // Replace "connectionItem" with "editorInput",just keep one instance which is from editorInput
    private IEditorInput editorInput = null;

    public ConnectionInfoPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
    }

    @Override
    protected ModelElement getCurrentModelElement(FormEditor editor) {
        this.editorInput = editor.getEditorInput();
        Connection connection = null;
        if (editorInput instanceof ConnectionItemEditorInput) {
            ConnectionItemEditorInput input = (ConnectionItemEditorInput) editorInput;
            connection = ((ConnectionItem) input.getItem()).getConnection();
        } else if (editorInput instanceof FileEditorInput) {// open from task
            Property proty = PropertyHelper.getProperty(((FileEditorInput) editorInput).getFile());
            // String fileLabel = proty.getLabel();
            // TDQ-11312
            if (proty == null) {
                IFile file = ((FileEditorInput) editorInput).getFile();
                IPath fullPath = file.getFullPath();
                String replace = fullPath.lastSegment().replace(this.oldDataproviderName, nameText.getText().trim());
                IPath removeLastSegments = fullPath.removeLastSegments(1);
                IPath newPath = removeLastSegments.append(replace);
                IFile file2 = ResourcesPlugin.getWorkspace().getRoot().getFile(newPath);

                editorInput = new FileEditorInput(file2);
                this.setInput(editorInput);
                proty = PropertyHelper.getProperty(((FileEditorInput) editorInput).getFile());
            }

            Item item = proty.getItem();
            if (item instanceof ConnectionItem) {
                connection = ((ConnectionItem) item).getConnection();
            }
        }
        initConnectionRepNode(connection);
        // MOD gdbu 2011-7-12 bug : 22598
        // TOS should use the common filler API to create the metadata objects,then TOP don't complement again.
        return connection;
        // ~22598
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        setFormTitle(DefaultMessagesImpl.getString("ConnectionInfoPage.connectionSettings")); //$NON-NLS-1$
        setMetadataSectionTitle(DefaultMessagesImpl.getString("ConnectionInfoPage.connectionMetadata")); //$NON-NLS-1$
        setMetadataSectionDescription(DefaultMessagesImpl.getString("ConnectionInfoPage.propertiesOConnnection")); //$NON-NLS-1$
        super.createFormContent(managedForm);

        createInformationSection(form, topComp);

        Button checkBtn = toolkit.createButton(topComp, DefaultMessagesImpl.getString("ConnectionInfoPage.check"), SWT.NONE); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.verticalSpan = 20;
        gd.horizontalAlignment = SWT.CENTER;
        checkBtn.setLayoutData(gd);

        checkBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    ReturnCode code = checkDBConnection();
                    if (code.isOk()) {
                        MessageDialog.openInformation(
                                null,
                                DefaultMessagesImpl.getString("ConnectionInfoPage.checkConnections"), DefaultMessagesImpl.getString("ConnectionInfoPage.checkConnectionSuccessful")); //$NON-NLS-1$ //$NON-NLS-2$
                    } else {
                        MessageDialog.openWarning(
                                null,
                                DefaultMessagesImpl.getString("ConnectionInfoPage.checkConnection"), DefaultMessagesImpl.getString("ConnectionInfoPage.CheckConnectionFailure", code.getMessage())); //$NON-NLS-1$ //$NON-NLS-2$ 
                    }
                } catch (Exception e2) {
                    MessageDialog.openWarning(
                            null,
                            DefaultMessagesImpl.getString("ConnectionInfoPage.checkConnection"), DefaultMessagesImpl.getString("ConnectionInfoPage.CheckConnectionFailure", e2.getMessage())); //$NON-NLS-1$ //$NON-NLS-2$ 
                }
            }

        });
    }

    /**
     * @param form
     * @param toolkit
     * @param topComp
     */
    void createInformationSection(final ScrolledForm form, Composite topComp) {
        infomatioinSection = createSection(
                form,
                topComp,
                DefaultMessagesImpl.getString("ConnectionInfoPage.connectionInformations"), DefaultMessagesImpl.getString("ConnectionInfoPage.informationsOfConnection")); //$NON-NLS-1$ //$NON-NLS-2$

        Composite sectionClient = toolkit.createComposite(infomatioinSection);
        sectionClient.setLayout(new GridLayout(2, false));
        Label loginLabel = new Label(sectionClient, SWT.NONE);
        loginLabel.setText(DefaultMessagesImpl.getString("ConnectionInfoPage.Login")); //$NON-NLS-1$

        loginText = new Text(sectionClient, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(loginText);
        Label passwordLabel = new Label(sectionClient, SWT.NONE);
        passwordLabel.setText(DefaultMessagesImpl.getString("ConnectionInfoPage.Password")); //$NON-NLS-1$
        passwordText = new Text(sectionClient, SWT.BORDER | SWT.PASSWORD);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(passwordText);

        Label urlLabel = new Label(sectionClient, SWT.NONE);
        urlLabel.setText(DefaultMessagesImpl.getString("ConnectionInfoPage.Url")); //$NON-NLS-1$

        Composite urlComp = new Composite(sectionClient, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        urlComp.setLayout(gridLayout);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(urlComp);
        urlText = new Text(urlComp, SWT.BORDER | SWT.READ_ONLY);
        GridDataFactory.fillDefaults().hint(100, -1).grab(true, true).applyTo(urlText);

        editButton = new Button(urlComp, SWT.PUSH);
        editButton.setText(DefaultMessagesImpl.getString("IndicatorDefinitionMaterPage.editExpression")); //$NON-NLS-1$
        editButton.setToolTipText(DefaultMessagesImpl.getString("IndicatorDefinitionMaterPage.editExpression")); //$NON-NLS-1$
        editButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                changeConnectionInformations();
            }
        });

        initConnInfoTextField();
        // MOD klliu 2010-07-06 bug 14095
        loginText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                if (!isRefreshText) {
                    setDirty(true);
                    isLoginChanged = true;
                }
            }

        });
        passwordText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                if (!isRefreshText) {
                    setDirty(true);
                    isPassWordChanged = true;
                }
            }

        });
        urlText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                if (!isRefreshText) {
                    setDirty(true);
                    isUrlChanged = true;
                }
                // saveTextChange();
            }

        });
        infomatioinSection.setClient(sectionClient);
    }

    /**
     * Change connection informations with server, port etc., and update related analyses.
     * 
     * MOD yyi 9082 2010-02-25
     */
    protected void changeConnectionInformations() {
        ConnectionItem connItem = getConnectionItem();
        if (connItem != null) {
            // MOD mzhao bug:19288
            if (connItem.eIsProxy()) {
                Property property = this.getProperty();
                if (property != null && property.getItem() != null) {
                    connItem = (ConnectionItem) property.getItem();
                }
            }
            RepositoryNode node = RepositoryNodeHelper.recursiveFind(connItem.getProperty());

            IWizard wizard = null;
            if (node != null) {
                if (node instanceof DBConnectionRepNode) {
                    wizard = new DatabaseWizard(PlatformUI.getWorkbench(), false, node, null);
                }
            }

            if (wizard != null) {
                WizardDialog dialog = new WizardDialog(null, wizard);
                dialog.setPageSize(550, 550);
                wizard.setContainer(dialog);
                dialog.open();
            }
        }
    }

    // MOD yyin 20121213, use the MetadataConnectionUtils to check the connection to replace the
    // org.talend.cwm.db.connection.ConnectionUtils.checkCOnnection method
    private ReturnCode checkDBConnection() {
        Properties props = new Properties();
        // MOD qiongli 2011-9-5 feature TDQ-3317,handle context model
        String userName = loginText.getText();
        String password = passwordText.getText();
        ConnectionItem connItem = getConnectionItem();
        ReturnCode returnCode = null;
        if (connItem != null) {
            Connection connection = connItem.getConnection();
            if (connection == null) {
                return new ReturnCode("connection is null!", false); //$NON-NLS-1$
            }
            if (connection.isContextMode()) {
                userName = JavaSqlFactory.getUsername(connection);
                password = JavaSqlFactory.getPassword(connection);
            }
            props.put(TaggedValueHelper.USER, userName);
            props.put(TaggedValueHelper.PASSWORD, password);

            returnCode = MetadataConnectionUtils.checkConnection((DatabaseConnection) connection);
        }

        return returnCode;
    }

    @Override
    public void setDirty(boolean isDirty) {
        if (this.isDirty != isDirty) {
            this.isDirty = isDirty;
            ((ConnectionEditor) this.getEditor()).firePropertyChange(IEditorPart.PROP_DIRTY);
        }

    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // ADD yyi 2011-05-31 16158:add whitespace check for text fields.
        if (!checkWhithspace()) {
            MessageUI.openError(DefaultMessagesImpl.getString("AbstractMetadataFormPage.whitespace")); //$NON-NLS-1$
            return;
        }
        if (!canSave().isOk()) {
            return;
        }
        if (editorInput instanceof FileEditorInput) {
            editorInput = this.getEditorInput();
        }

        // MOD qiongi 2013-5-17 delete some code of checkconnection.no need to check connection at here
        if (!impactAnalyses().isOk()) {
            return;
        }

        super.doSave(monitor);

        try {

            // MOD sizhaoliu TDQ-6296 open an analysis after renaming the connection on which it depends, connection
            // field is empty and all the indicators are lost.
            // the following instruction should be called after reloadDataProvider()
            saveConnectionInfo();

            this.initialize(this.getEditor());

            this.isUrlChanged = false;
            this.isLoginChanged = false;
            this.isPassWordChanged = false;
            this.isDirty = false;
        } catch (DataprofilerCoreException e) {
            ExceptionHandler.process(e, Level.ERROR);
        }
    }

    /**
     * DOC yyi Comment method "impactAnalyses".
     */
    private ReturnCode impactAnalyses() {

        ReturnCode rc = new ReturnCode();
        String dialogMessage = DefaultMessagesImpl.getString("ConnectionInfoPage.impactAnalyses");//$NON-NLS-1$
        String dialogTitle = DefaultMessagesImpl.getString("ConnectionInfoPage.warningTitle");//$NON-NLS-1$
        ConnectionItem connItem = getConnectionItem();
        if (connItem != null) {
            Connection connection = connItem.getConnection();
            // MOD klliu 2010-07-06 bug 14095: unnecessary wizard
            if (connection != null && (this.isUrlChanged || this.isLoginChanged || this.isPassWordChanged)) {
                rc.setOk(Window.OK == DeleteModelElementConfirmDialog.showElementImpactConfirmDialog(null,
                        new ModelElement[] { connection }, dialogTitle, dialogMessage));
            }
        }

        return rc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.AbstractMetadataFormPage#saveTextChange()
     */
    @Override
    protected boolean saveTextChange() {
        // get the last version element if it is proxy.
        ConnectionItem connItem = getConnectionItem();
        if (connItem != null) {
            Connection connection = connItem.getConnection();
            if (connection == null) {
                return false;
            }
            if (connItem.eIsProxy()) {
                Property property = this.getProperty();
                if (property != null && property.getItem() != null) {
                    connItem = (ConnectionItem) property.getItem();
                }
                if (connItem != null) {
                    connection = connItem.getConnection();
                }
            }

            if (!connection.isContextMode()) {
                JavaSqlFactory.setUsername(connection, loginText.getText());
                JavaSqlFactory.setPassword(connection, passwordText.getText());
            }
            // MOD msjian 2011-7-18 23216: when there is no error for name, do set
            if (super.saveTextChange()) {
                ConnectionUtils.setName(connection, nameText.getText());
                // MOD zshen for bug 4314 I think there not need this set method for displayName
                // PropertyHelper.getProperty(connection).setDisplayName(nameText.getText());
                // PropertyHelper.getProperty(connection).setLabel(nameText.getText());
            } else {
                return false;
            }
        }
        return true;
    }

    private void saveConnectionInfo() throws DataprofilerCoreException {
        ConnectionItem connItem = getConnectionItem();
        if (connItem != null) {
            Connection connection = connItem.getConnection();
            if (connection == null) {
                return;
            }
            ConnectionUtils.checkUsernameBeforeSaveConnection4Sqlite(connection);

            ReturnCode returnCode = ElementWriterFactory.getInstance().createDataProviderWriter().save(connItem, true);
            if (editorInput instanceof FileEditorInput) {
                this.currentModelElement = connection;
            }
            if (returnCode.isOk()) {
                if (log.isDebugEnabled()) {
                    log.debug("Saved in  " + connection.eResource().getURI().toFileString() + " successful"); //$NON-NLS-1$ //$NON-NLS-2$
                }

            } else {
                throw new DataprofilerCoreException(
                        DefaultMessagesImpl
                                .getString(
                                        "ConnectionInfoPage.ProblemSavingFile", connection.eResource().getURI().toFileString(), returnCode.getMessage())); //$NON-NLS-1$
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.AbstractMetadataFormPage#canSave()
     */
    @Override
    public ReturnCode canSave() {
        ReturnCode rc = canModifyName(ERepositoryObjectType.METADATA_CONNECTIONS);
        if (!rc.isOk()) {
            MessageDialogWithToggle.openError(null,
                    DefaultMessagesImpl.getString("AbstractMetadataFormPage.saveFailed"), rc.getMessage()); //$NON-NLS-1$
        }
        return rc;
    }

    public void refreshTextInfo() {
        isRefreshText = true;
        initMetaTextFied();
        initConnInfoTextField();
        isRefreshText = false;
        setDirty(false);

    }

    private void initConnInfoTextField() {
        ConnectionItem connItem = getConnectionItem();
        if (connItem != null) {
            Connection connection = connItem.getConnection();
            if (connection == null) {
                return;
            }
            String loginValue = JavaSqlFactory.getUsername(connection);
            loginText.setText(loginValue == null ? PluginConstant.EMPTY_STRING : loginValue);
            loginText.setEditable(!connection.isContextMode());
            // MOD scorreia 2009-01-09 handle encrypted password
            String passwordValue = JavaSqlFactory.getPassword(connection);
            passwordText.setText(passwordValue == null ? PluginConstant.EMPTY_STRING : passwordValue);
            passwordText.setEditable(!connection.isContextMode());

            String urlValue = JavaSqlFactory.getURL(connection);
            urlText.setText(urlValue == null ? PluginConstant.EMPTY_STRING : urlValue);
            String driverClass = JavaSqlFactory.getDriverClass(connection);
            if (driverClass != null && driverClass.startsWith("org.sqlite")) { //$NON-NLS-1$
                loginText.setEnabled(false);
                passwordText.setEnabled(false);
            }
        }
    }

    /**
     * 
     * make sure the connection item in this page just has one instance and it is from RepositoryNode.
     * 
     * @return
     */
    private ConnectionItem getConnectionItem() {
        ConnectionItem item = null;
        if (editorInput == null) {
            editorInput = getEditorInput();
        }
        if (editorInput instanceof ConnectionItemEditorInput) {
            ConnectionItemEditorInput input = (ConnectionItemEditorInput) editorInput;
            item = (ConnectionItem) input.getItem();
        } else if (editorInput instanceof FileEditorInput) {
            Property proty = PropertyHelper.getProperty(((FileEditorInput) editorInput).getFile());
            if (proty != null && proty.getItem() != null) {
                item = (ConnectionItem) proty.getItem();
            }
        }
        return item;
    }
}
