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
package org.talend.dataprofiler.core.ui.editor.analysis;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.talend.commons.utils.WorkspaceUtils;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.TableIndicator;
import org.talend.dataprofiler.core.ui.action.actions.RunAnalysisAction;
import org.talend.dataprofiler.core.ui.chart.ChartUtils;
import org.talend.dataprofiler.core.ui.editor.analysis.TablesSelectionDialog.TableSelectionType;
import org.talend.dataprofiler.core.ui.editor.composite.AbstractColumnDropTree;
import org.talend.dataprofiler.core.ui.editor.composite.AnalysisTableTreeViewer;
import org.talend.dataprofiler.core.ui.editor.composite.DataFilterComp;
import org.talend.dataprofiler.core.ui.editor.preview.CompositeIndicator;
import org.talend.dataprofiler.core.ui.editor.preview.TableIndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTypeStatesOperator;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.IChartTypeStates;
import org.talend.dataprofiler.core.ui.pref.EditorPreferencePage;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.ExecutionLanguage;
import org.talend.dataquality.domain.Domain;
import org.talend.dataquality.exception.DataprofilerCoreException;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.properties.TDQAnalysisItem;
import org.talend.dataquality.rules.DQRule;
import org.talend.dq.analysis.TableAnalysisHandler;
import org.talend.dq.helper.EObjectHelper;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.indicators.preview.EIndicatorChartType;
import org.talend.dq.writer.impl.ElementWriterFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.utils.sugars.ReturnCode;
import orgomg.cwm.objectmodel.core.ModelElement;
import orgomg.cwm.resource.relational.NamedColumnSet;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class TableMasterDetailsPage extends AbstractAnalysisMetadataPage implements PropertyChangeListener {

    private static Logger log = Logger.getLogger(TableMasterDetailsPage.class);

    private String execLang = ExecutionLanguage.SQL.getLiteral();

    AnalysisTableTreeViewer treeViewer;

    DataFilterComp dataFilterComp;

    TableAnalysisHandler analysisHandler;

    public TableAnalysisHandler getAnalysisHandler() {
        return analysisHandler;
    }

    private TableIndicator[] currentTableIndicators;

    private String stringDataFilter;

    private Composite chartComposite;

    private ScrolledForm form;

    public ScrolledForm getForm() {
        return form;
    }

    public void setForm(ScrolledForm form) {
        this.form = form;
    }

    private static final int TREE_MAX_LENGTH = 400;

    private Composite[] previewChartCompsites;

    public Composite[] getPreviewChartCompsites() {
        return previewChartCompsites;
    }

    public void setPreviewChartCompsites(Composite[] previewChartCompsites) {
        this.previewChartCompsites = previewChartCompsites;
    }

    private AnalysisEditor currentEditor;

    private Section dataFilterSection = null;

    private Section analysisTableSection = null;

    private Section previewSection = null;

    private List<ExpandableComposite> previewChartList = null;

    private SashForm sForm;

    private Composite previewComp;

    /**
     * DOC xqliu TableMasterDetailsPage constructor comment.
     * 
     * @param editor
     * @param id
     * @param title
     */
    public TableMasterDetailsPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        currentEditor = (AnalysisEditor) editor;
    }

    public TableIndicator[] getCurrentTableIndicators() {
        return currentTableIndicators;
    }

    public void initialize(FormEditor editor) {
        super.initialize(editor);
        recomputeIndicators();
    }

    public void recomputeIndicators() {
        analysisHandler = new TableAnalysisHandler();
        analysisHandler.setAnalysis((Analysis) this.currentModelElement);
        stringDataFilter = analysisHandler.getStringDataFilter();
        EList<ModelElement> analyzedTables = analysisHandler.getAnalyzedTables();
        List<TableIndicator> tableIndicatorList = new ArrayList<TableIndicator>();
        for (ModelElement element : analyzedTables) {

            NamedColumnSet set = SwitchHelpers.NAMED_COLUMN_SET_SWITCH.doSwitch(element);
            if (set == null) {
                continue;
            }

            TableIndicator currentTableIndicator = new TableIndicator(set);
            Collection<Indicator> indicatorList = analysisHandler.getIndicators(set);
            currentTableIndicator.setIndicators(indicatorList.toArray(new Indicator[indicatorList.size()]));
            tableIndicatorList.add(currentTableIndicator);
        }
        currentTableIndicators = tableIndicatorList.toArray(new TableIndicator[tableIndicatorList.size()]);
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        this.form = managedForm.getForm();
        Composite body = form.getBody();

        body.setLayout(new GridLayout());
        sForm = new SashForm(body, SWT.NULL);
        sForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        topComp = toolkit.createComposite(sForm);
        topComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        topComp.setLayout(new GridLayout());
        metadataSection = creatMetadataSection(form, topComp);
        form.setText(DefaultMessagesImpl.getString("TableMasterDetailsPage.tableAna")); //$NON-NLS-1$
        metadataSection.setText(DefaultMessagesImpl.getString("TableMasterDetailsPage.analysisMeta")); //$NON-NLS-1$
        metadataSection.setDescription(DefaultMessagesImpl.getString("TableMasterDetailsPage.setPropOfAnalysis")); //$NON-NLS-1$

        createAnalysisTablesSection(form, topComp);

        createDataFilterSection(form, topComp);
        // MOD klliu Hide the setting pate graphics 2011-03-11
        if (!EditorPreferencePage.isHideGraphics()) {
            previewComp = toolkit.createComposite(sForm);
            previewComp.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewComp.setLayout(new GridLayout());
            // add by hcheng for 0007290: Chart cannot auto compute it's size in
            // DQRule analsyis Editor
            previewComp.addControlListener(new ControlAdapter() {

                /*
                 * (non-Javadoc)
                 * 
                 * @see org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse .swt.events.ControlEvent)
                 */
                @Override
                public void controlResized(ControlEvent e) {
                    super.controlResized(e);
                    sForm.redraw();
                    form.reflow(true);
                }
            });

            createPreviewSection(form, previewComp);
        }
    }

    void createAnalysisTablesSection(final ScrolledForm form, Composite anasisDataComp) {
        analysisTableSection = createSection(form, anasisDataComp,
                DefaultMessagesImpl.getString("TableMasterDetailsPage.analyzeTable"), null); //$NON-NLS-1$

        Composite topComp = toolkit.createComposite(analysisTableSection);
        topComp.setLayout(new GridLayout());
        // ~ MOD mzhao 2009-05-05,Bug 6587.
        createConnBindWidget(topComp);
        // ~
        Hyperlink tblBtn = toolkit.createHyperlink(topComp,
                DefaultMessagesImpl.getString("TableMasterDetailsPage.selectTable"), SWT.NONE); //$NON-NLS-1$
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(tblBtn);
        tblBtn.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                openTableSelectionDialog();
            }

        });

        Composite actionBarComp = toolkit.createComposite(topComp);
        GridLayout gdLayout = new GridLayout();
        gdLayout.numColumns = 2;
        actionBarComp.setLayout(gdLayout);

        ImageHyperlink collapseAllImageLink = toolkit.createImageHyperlink(actionBarComp, SWT.NONE);
        collapseAllImageLink.setToolTipText(DefaultMessagesImpl.getString("TableMasterDetailsPage.collapseAllTables")); //$NON-NLS-1$
        collapseAllImageLink.setImage(ImageLib.getImage(ImageLib.COLLAPSE_ALL));
        collapseAllImageLink.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                TreeItem[] items = treeViewer.getTree().getItems();
                expandTreeItems(items, false);
                packOtherColumns();
            }
        });

        ImageHyperlink expandAllImageLink = toolkit.createImageHyperlink(actionBarComp, SWT.NONE);
        expandAllImageLink.setToolTipText(DefaultMessagesImpl.getString("TableMasterDetailsPage.expandAllTables")); //$NON-NLS-1$
        expandAllImageLink.setImage(ImageLib.getImage(ImageLib.EXPAND_ALL));
        expandAllImageLink.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                TreeItem[] items = treeViewer.getTree().getItems();
                expandTreeItems(items, true);
                packOtherColumns();

            }
        });

        Composite treeComp = toolkit.createComposite(topComp, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(treeComp);
        treeComp.setLayout(new GridLayout());
        ((GridData) treeComp.getLayoutData()).heightHint = TREE_MAX_LENGTH;

        treeViewer = new AnalysisTableTreeViewer(treeComp, this);
        treeViewer.setDirty(false);
        treeViewer.addPropertyChangeListener(this);
        analysisTableSection.setClient(topComp);

    }

    /**
     * 
     * DOC xqliu Comment method "packOtherColumns".
     */
    private void packOtherColumns() {
        TreeColumn[] columns = treeViewer.getTree().getColumns();
        for (TreeColumn column : columns) {
            column.pack();
        }
    }

    /**
     * 
     * DOC xqliu Comment method "expandTreeItems".
     * 
     * @param items
     * @param expandOrCollapse
     */
    private void expandTreeItems(TreeItem[] items, boolean expandOrCollapse) {
        for (TreeItem item : items) {
            item.setExpanded(expandOrCollapse);
            TreeItem[] its = item.getItems();
            if (its != null && its.length > 0) {
                expandTreeItems(its, expandOrCollapse);
            }
        }
    }

    public void openTableSelectionDialog() {
        TableIndicator[] tableIndicators = treeViewer.getTableIndicator();
        List<IRepositoryNode> setList = new ArrayList<IRepositoryNode>();
        for (TableIndicator tableIndicator : tableIndicators) {
            setList.add(RepositoryNodeHelper.recursiveFind(tableIndicator.getColumnSet()));
        }
        TablesSelectionDialog dialog = new TablesSelectionDialog(this, null,
                DefaultMessagesImpl.getString("TableMasterDetailsPage.tableSelection"), setList, DefaultMessagesImpl //$NON-NLS-1$
                        .getString("TableMasterDetailsPage.tableSelections")); //$NON-NLS-1$
        dialog.setTableType(TableSelectionType.ALL);
        if (dialog.open() == Window.OK) {
            Object[] tables = dialog.getResult();
            treeViewer.setInput(tables);
            return;
        }
    }

    /**
     * @param form
     * @param toolkit
     * @param anasisDataComp
     */
    void createDataFilterSection(final ScrolledForm form, Composite anasisDataComp) {
        dataFilterSection = createSection(
                form,
                anasisDataComp,
                DefaultMessagesImpl.getString("TableMasterDetailsPage.dataFilter"), DefaultMessagesImpl.getString("TableMasterDetailsPage.editDataFilter")); //$NON-NLS-1$ //$NON-NLS-2$

        Composite sectionClient = toolkit.createComposite(dataFilterSection);
        dataFilterComp = new DataFilterComp(sectionClient, stringDataFilter);
        dataFilterComp.addPropertyChangeListener(this);
        addWhitespaceValidate(dataFilterComp.getDataFilterText());
        dataFilterSection.setClient(sectionClient);
    }

    void createPreviewSection(final ScrolledForm form, Composite parent) {
        previewSection = createSection(
                form,
                parent,
                DefaultMessagesImpl.getString("TableMasterDetailsPage.graphics"), DefaultMessagesImpl.getString("TableMasterDetailsPage.space")); //$NON-NLS-1$ //$NON-NLS-2$
        previewSection.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite sectionClient = toolkit.createComposite(previewSection);
        sectionClient.setLayout(new GridLayout());
        sectionClient.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite actionBarComp = toolkit.createComposite(sectionClient);
        GridLayout gdLayout = new GridLayout();
        gdLayout.numColumns = 2;
        actionBarComp.setLayout(gdLayout);

        ImageHyperlink collapseAllImageLink = toolkit.createImageHyperlink(actionBarComp, SWT.NONE);
        collapseAllImageLink.setToolTipText(DefaultMessagesImpl.getString("TableMasterDetailsPage.collapseAllTables")); //$NON-NLS-1$
        collapseAllImageLink.setImage(ImageLib.getImage(ImageLib.COLLAPSE_ALL));
        collapseAllImageLink.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                if (previewChartList != null && !previewChartList.isEmpty()) {
                    for (ExpandableComposite comp : previewChartList) {
                        comp.setExpanded(false);
                        comp.getParent().pack();
                    }
                }
            }
        });

        ImageHyperlink expandAllImageLink = toolkit.createImageHyperlink(actionBarComp, SWT.NONE);
        expandAllImageLink.setToolTipText(DefaultMessagesImpl.getString("TableMasterDetailsPage.expandAllTables")); //$NON-NLS-1$
        expandAllImageLink.setImage(ImageLib.getImage(ImageLib.EXPAND_ALL));
        expandAllImageLink.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                if (previewChartList != null && !previewChartList.isEmpty()) {
                    for (ExpandableComposite comp : previewChartList) {
                        comp.setExpanded(true);
                        comp.getParent().pack();
                    }
                }

            }
        });

        ImageHyperlink refreshBtn = toolkit.createImageHyperlink(sectionClient, SWT.NONE);
        refreshBtn.setText(DefaultMessagesImpl.getString("TableMasterDetailsPage.refreshGraphics")); //$NON-NLS-1$
        refreshBtn.setImage(ImageLib.getImage(ImageLib.SECTION_PREVIEW));
        final Label message = toolkit.createLabel(sectionClient,
                DefaultMessagesImpl.getString("TableMasterDetailsPage.spaceWhite")); //$NON-NLS-1$
        message.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        message.setVisible(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(sectionClient);

        chartComposite = toolkit.createComposite(sectionClient);
        chartComposite.setLayout(new GridLayout());
        chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Analysis analysis = analysisHandler.getAnalysis();

        refreshBtn.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {

                for (Control control : chartComposite.getChildren()) {
                    control.dispose();
                }

                boolean analysisStatue = analysis.getResults().getResultMetadata() != null
                        && analysis.getResults().getResultMetadata().getExecutionDate() != null;

                if (!analysisStatue) {
                    boolean returnCode = MessageDialog.openConfirm(null,
                            DefaultMessagesImpl.getString("TableMasterDetailsPage.string0"), //$NON-NLS-1$
                            DefaultMessagesImpl.getString("TableMasterDetailsPage.string1")); //$NON-NLS-1$

                    if (returnCode) {
                        new RunAnalysisAction().run();
                        message.setVisible(false);
                    } else {
                        createPreviewCharts(form, chartComposite, false);
                        message.setText(DefaultMessagesImpl.getString("TableMasterDetailsPage.warning")); //$NON-NLS-1$
                        message.setVisible(true);
                    }
                } else {
                    createPreviewCharts(form, chartComposite, true);
                }

                chartComposite.layout();
                form.reflow(true);
            }

        });

        previewSection.setClient(sectionClient);
    }

    public void createPreviewCharts(final ScrolledForm form, final Composite composite, final boolean isCreate) {
        previewChartList = new ArrayList<ExpandableComposite>();
        for (final TableIndicator tableIndicator : this.treeViewer.getTableIndicator()) {
            final NamedColumnSet set = tableIndicator.getColumnSet();
            ExpandableComposite exComp = toolkit.createExpandableComposite(composite, ExpandableComposite.TREE_NODE
                    | ExpandableComposite.CLIENT_INDENT);
            // bug 10541 modify by zshen,Change some character set to be proper to add view in the table anasys
            if (tableIndicator.isTable()) {
                exComp.setText(DefaultMessagesImpl.getString("TableMasterDetailsPage.table") + set.getName()); //$NON-NLS-1$
            } else {
                exComp.setText(DefaultMessagesImpl.getString("TableMasterDetailsPage.view") + set.getName()); //$NON-NLS-1$
            }
            exComp.setLayout(new GridLayout());
            exComp.setLayoutData(new GridData(GridData.FILL_BOTH));
            exComp.setData(tableIndicator);
            previewChartList.add(exComp);

            final Composite comp = toolkit.createComposite(exComp);
            comp.setLayout(new GridLayout());
            comp.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (tableIndicator.getIndicators().length != 0) {
                IRunnableWithProgress rwp = new IRunnableWithProgress() {

                    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        monitor.beginTask(DefaultMessagesImpl.getString("TableMasterDetailsPage.createPreview") //$NON-NLS-1$
                                + set.getName(), IProgressMonitor.UNKNOWN);
                        Display.getDefault().syncExec(new Runnable() {

                            public void run() {
                                Map<EIndicatorChartType, List<TableIndicatorUnit>> indicatorComposite = CompositeIndicator
                                        .getInstance().getTableIndicatorComposite(tableIndicator);
                                for (EIndicatorChartType chartType : indicatorComposite.keySet()) {
                                    List<TableIndicatorUnit> units = indicatorComposite.get(chartType);
                                    if (!units.isEmpty()) {
                                        final IChartTypeStates chartTypeState = ChartTypeStatesOperator.getChartStateTable(
                                                chartType, units, tableIndicator);

                                        List<JFreeChart> charts = chartTypeState.getChartList();
                                        if (charts != null) {
                                            for (JFreeChart chart : charts) {
                                                final ChartComposite chartComp = new ChartComposite(comp, SWT.NONE, chart, true);

                                                GridData gd = new GridData();
                                                gd.widthHint = 550;
                                                gd.heightHint = 250;
                                                chartComp.setLayoutData(gd);

                                                addListenerToChartComp(chartComp, chartTypeState);
                                            }
                                        }
                                    }
                                }
                            }
                        });

                        monitor.done();
                    }

                };

                try {
                    new ProgressMonitorDialog(getSite().getShell()).run(true, false, rwp);
                } catch (Exception ex) {
                    log.error(ex, ex);
                }
            }

            exComp.addExpansionListener(new ExpansionAdapter() {

                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    getChartComposite().layout();
                    form.reflow(true);
                    composite.pack();
                }

            });

            exComp.setExpanded(true);
            exComp.setClient(comp);

            form.reflow(true);
        }
        if (!previewChartList.isEmpty()) {
            this.previewChartCompsites = previewChartList.toArray(new Composite[previewChartList.size()]);
        }
    }

    private void addListenerToChartComp(final ChartComposite chartComp, final IChartTypeStates chartTypeState) {
        chartComp.addChartMouseListener(new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent event) {
                final String referenceLink = chartTypeState.getReferenceLink();
                if (event.getTrigger().getButton() == 1 && referenceLink != null) {
                    Menu menu = new Menu(chartComp.getShell(), SWT.POP_UP);
                    chartComp.setMenu(menu);

                    MenuItem item = new MenuItem(menu, SWT.PUSH);
                    item.setText(DefaultMessagesImpl.getString("TableMasterDetailsPage.what")); //$NON-NLS-1$
                    item.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            ChartUtils.openReferenceLink(referenceLink);
                        }
                    });

                    menu.setVisible(true);
                }
            }

            public void chartMouseMoved(ChartMouseEvent event) {

            }

        });
    }

    public Composite getChartComposite() {
        return chartComposite;
    }

    @Override
    protected ReturnCode canRun() {
        TableIndicator[] tableIndicators = treeViewer.getTableIndicator();
        if (tableIndicators == null || tableIndicators.length == 0) {
            return new ReturnCode(DefaultMessagesImpl.getString("TableMasterDetailsPage.NoTableAssigned"), false); //$NON-NLS-1$
        }
        for (TableIndicator tableIndicator : tableIndicators) {
            if (tableIndicator.getIndicators().length == 0) {
                return new ReturnCode(DefaultMessagesImpl.getString("TableMasterDetailsPage.NoIndicatorAssigned"), false); //$NON-NLS-1$
            }
        }
        return new ReturnCode(true);
    }

    @Override
    public ReturnCode canSave() {

        // MOD by gdbu 2011-3-21 bug 19179
        ReturnCode canModRetCode = super.canSave();
        if (!canModRetCode.isOk()) {
            return canModRetCode;
        }
        // ~19179

        return new ReturnCode(true);
    }

    @Override
    public void saveAnalysis() throws DataprofilerCoreException {
        // ADD gdbu 2011-3-3 bug 19179
        List<DQRule> oldDqRules = getDqRules(analysis);
        // remove the space from analysis name
        //        analysis.setName(analysis.getName().replace(" ", ""));//$NON-NLS-1$ //$NON-NLS-2$
        for (Domain domain : this.analysis.getParameters().getDataFilter()) {
            domain.setName(this.analysis.getName());
        }
        // ~

        IRepositoryViewObject reposObject = null;
        analysisHandler.clearAnalysis();
        TableIndicator[] tableIndicators = treeViewer.getTableIndicator();
        Connection tdProvider = null;
        Analysis analysis = analysisHandler.getAnalysis();
        analysis.getParameters().setExecutionLanguage(ExecutionLanguage.get(execLang));
        if (tableIndicators != null && tableIndicators.length != 0) {

            tdProvider = ConnectionHelper.getDataProvider(tableIndicators[0].getColumnSet());
            if (tdProvider.eIsProxy()) {
                // Resolve the connection again
                tdProvider = (Connection) EObjectHelper.resolveObject(tdProvider);
            }
            analysis.getContext().setConnection(tdProvider);
            for (TableIndicator tableIndicator : tableIndicators) {
                analysisHandler.addIndicator(tableIndicator.getColumnSet(), tableIndicator.getIndicators());
            }
        } else {
            deleteConnectionDependency(analysis);
        }
        analysisHandler.setStringDataFilter(dataFilterComp.getDataFilterString());
        // ADD xqliu 2010-07-19 bug 14014
        this.updateAnalysisClientDependency();
        // ~ 14014
        // 2011.1.12 MOD by zhsne to unify anlysis and connection id when saving.
        ReturnCode saved = new ReturnCode(false);
        IEditorInput editorInput = this.getEditorInput();
        if (editorInput instanceof AnalysisItemEditorInput) {
            AnalysisItemEditorInput analysisInput = (AnalysisItemEditorInput) editorInput;
            TDQAnalysisItem tdqAnalysisItem = analysisInput.getTDQAnalysisItem();

            // ADD gdbu 2011-3-3 bug 19179
            tdqAnalysisItem.getProperty().setDisplayName(analysisHandler.getName());
            tdqAnalysisItem.getProperty().setLabel(WorkspaceUtils.normalize(analysisHandler.getName()));
            this.nameText.setText(analysisHandler.getName());
            // ~
            // MOD yyi 2012-02-08 TDQ-4621:Explicitly set true for updating dependencies.
            saved = ElementWriterFactory.getInstance().createAnalysisWrite().save(tdqAnalysisItem, true);
        }
        // MOD yyi 2012-02-03 TDQ-3602:Avoid to rewriting all analyzes after saving, no reason to update all analyzes
        // which is depended in the referred connection.
        // Extract saving log function.
        // @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#logSaved(ReturnCode)
        logSaved(saved);

        treeViewer.setDirty(false);
        dataFilterComp.setDirty(false);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            currentEditor.firePropertyChange(IEditorPart.PROP_DIRTY);
            currentEditor.setRefreshResultPage(true);
        } else if (PluginConstant.DATAFILTER_PROPERTY.equals(evt.getPropertyName())) {
            this.analysisHandler.setStringDataFilter((String) evt.getNewValue());
        }
    }

    @Override
    public void refresh() {
        // if (chartComposite != null) {
        // try {
        // for (Control control : chartComposite.getChildren()) {
        // control.dispose();
        // }
        // createPreviewCharts(form, chartComposite, true);
        // chartComposite.getParent().layout();
        // chartComposite.layout();
        // } catch (Exception ex) {
        // log.error(ex, ex);
        // }
        // }
        if (EditorPreferencePage.isHideGraphics()) {
            if (sForm.getChildren().length > 1) {
                if (null != sForm.getChildren()[1] && !sForm.getChildren()[1].isDisposed())
                    sForm.getChildren()[1].dispose();
                topComp.getParent().layout();
                topComp.layout();
            }

        } else {
            if (chartComposite != null && !chartComposite.isDisposed()) {
                try {
                    for (Control control : chartComposite.getChildren()) {
                        control.dispose();
                    }
                    createPreviewCharts(form, chartComposite, true);
                    chartComposite.getParent().layout();
                    chartComposite.layout();
                } catch (Exception ex) {
                    log.error(ex, ex);
                }
            } else {
                previewComp = toolkit.createComposite(sForm);
                previewComp.setLayoutData(new GridData(GridData.FILL_BOTH));
                previewComp.setLayout(new GridLayout());
                previewComp.addControlListener(new ControlAdapter() {

                    public void controlResized(ControlEvent e) {
                        super.controlResized(e);
                        sForm.redraw();
                        form.reflow(true);
                    }
                });
                createPreviewSection(form, previewComp);
                createPreviewCharts(form, chartComposite, true);
            }
        }
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || (treeViewer != null && treeViewer.isDirty())
                || (dataFilterComp != null && dataFilterComp.isDirty());
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.treeViewer != null) {
            this.treeViewer.removePropertyChangeListener(this);
        }
        if (dataFilterComp != null) {
            this.dataFilterComp.removePropertyChangeListener(this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#getTreeViewer()
     */
    @Override
    public AbstractColumnDropTree getTreeViewer() {
        return this.treeViewer;
    }
}