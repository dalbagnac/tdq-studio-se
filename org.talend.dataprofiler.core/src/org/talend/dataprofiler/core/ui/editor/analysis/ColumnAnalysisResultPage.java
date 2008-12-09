// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
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

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.relational.TdColumn;
import org.talend.cwm.softwaredeployment.TdDataProvider;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.ColumnIndicator;
import org.talend.dataprofiler.core.ui.editor.preview.CompositeIndicator;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableFactory;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableMenuGenerator;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTypeStatesOperator;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartWithData;
import org.talend.dataprofiler.core.ui.editor.preview.model.ICustomerDataset;
import org.talend.dataprofiler.core.ui.editor.preview.model.MenuItemEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.IChartTypeStates;
import org.talend.dataprofiler.core.ui.utils.ChartUtils;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.PatternFreqIndicator;
import org.talend.dataquality.indicators.PatternLowFreqIndicator;
import org.talend.dq.analysis.AnalysisHandler;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.analysis.explore.IDataExplorer;
import org.talend.dq.dbms.DbmsLanguageFactory;
import org.talend.dq.indicators.preview.EIndicatorChartType;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.pattern.PatternTransformer;

/**
 * DOC zqin class global comment. Detailled comment
 */
public class ColumnAnalysisResultPage extends AbstractAnalysisResultPage implements PropertyChangeListener {

    private Composite resultComp;

    ColumnMasterDetailsPage masterPage;

    /**
     * DOC zqin ColumnAnalysisResultPage constructor comment.
     * 
     * @param editor
     * @param id
     * @param title
     */
    public ColumnAnalysisResultPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        AnalysisEditor analysisEditor = (AnalysisEditor) editor;
        this.masterPage = (ColumnMasterDetailsPage) analysisEditor.getMasterPage();
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);

        resultComp = toolkit.createComposite(topComposite);
        resultComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        resultComp.setLayout(new GridLayout());
        createResultSection(resultComp);

        form.reflow(true);
    }

    @Override
    protected AnalysisHandler getColumnAnalysisHandler() {
        return this.masterPage.getAnalysisHandler();
    }

    protected void createResultSection(Composite parent) {
        Section section = createSection(form, parent,
                DefaultMessagesImpl.getString("ColumnAnalysisResultPage.analysisResult"), true, null); //$NON-NLS-1$
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new GridLayout());
        sectionClient.setLayoutData(new GridData(GridData.FILL_BOTH));

        for (final ColumnIndicator columnIndicator : masterPage.getTreeViewer().getColumnIndicator()) {

            ExpandableComposite exComp = toolkit.createExpandableComposite(sectionClient, ExpandableComposite.TWISTIE
                    | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.EXPANDED);
            exComp
                    .setText(DefaultMessagesImpl.getString("ColumnAnalysisResultPage.column") + columnIndicator.getTdColumn().getName()); //$NON-NLS-1$
            exComp.setLayout(new GridLayout());
            exComp.setLayoutData(new GridData(GridData.FILL_BOTH));

            final Composite comp = toolkit.createComposite(exComp);
            comp.setLayout(new GridLayout());
            comp.setLayoutData(new GridData(GridData.FILL_BOTH));
            exComp.setClient(comp);

            createResultDataComposite(comp, columnIndicator);

            exComp.addExpansionListener(new ExpansionAdapter() {

                public void expansionStateChanged(ExpansionEvent e) {

                    form.reflow(true);
                }

            });
        }

        section.setClient(sectionClient);
    }

    private void createResultDataComposite(final Composite comp, final ColumnIndicator columnIndicator) {
        if (columnIndicator.getIndicators().length != 0) {

            final TdColumn column = columnIndicator.getTdColumn();
            IRunnableWithProgress rwp = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

                    monitor
                            .beginTask(
                                    DefaultMessagesImpl.getString("ColumnAnalysisResultPage.createPreview") + column.getName(), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {

                            Map<EIndicatorChartType, List<IndicatorUnit>> indicatorComposite = CompositeIndicator.getInstance()
                                    .getIndicatorComposite(columnIndicator);
                            for (EIndicatorChartType chartType : indicatorComposite.keySet()) {
                                List<IndicatorUnit> units = indicatorComposite.get(chartType);
                                if (!units.isEmpty()) {
                                    IChartTypeStates chartTypeState = ChartTypeStatesOperator.getChartState(chartType, units);
                                    ChartWithData chartData = new ChartWithData(chartType, chartTypeState.getChart(),
                                            chartTypeState.getDataEntity());

                                    // create UI
                                    ExpandableComposite subComp = toolkit.createExpandableComposite(comp,
                                            ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT
                                                    | ExpandableComposite.EXPANDED);
                                    subComp.setText(chartData.getChartType().getLiteral());
                                    subComp.setLayoutData(new GridData(GridData.FILL_BOTH));

                                    final Composite composite = toolkit.createComposite(subComp, SWT.NULL);
                                    composite.setLayout(new GridLayout(2, false));
                                    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

                                    Analysis analysis = masterPage.getAnalysisHandler().getAnalysis();

                                    // create table
                                    TableViewer tableviewer = chartTypeState.getTableForm(composite);
                                    tableviewer.setInput(chartData);
                                    DataExplorer dataExplorer = chartTypeState.getDataExplorer();
                                    ChartTableFactory.addMenuAndTip(tableviewer, dataExplorer, analysis);

                                    // create chart
                                    JFreeChart chart = chartTypeState.getChart();
                                    if (chart != null) {
                                        GridData gd = new GridData(GridData.FILL_BOTH);
                                        ChartPanel chartPanel = ChartUtils.createAWTSWTComp(composite, gd, chart);
                                        addMouseListenerForChart(chartPanel, dataExplorer, analysis);
                                    }

                                    subComp.setClient(composite);
                                    subComp.addExpansionListener(new ExpansionAdapter() {

                                        @Override
                                        public void expansionStateChanged(ExpansionEvent e) {
                                            form.reflow(true);
                                        }

                                    });
                                }
                            }
                        }

                    });

                    monitor.done();
                }

            };

            try {
                new ProgressMonitorDialog(null).run(true, false, rwp);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.AbstractFormPage#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            ((AnalysisEditor) this.getEditor()).firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void refresh(ColumnMasterDetailsPage masterPage) {
        this.masterPage = masterPage;
        this.summaryComp.dispose();
        this.resultComp.dispose();

        createFormContent(getManagedForm());
    }

    private void addMouseListenerForChart(final ChartPanel chartPanel, final IDataExplorer explorer, final Analysis analysis) {
        chartPanel.addChartMouseListener(new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity chartEntity = event.getEntity();
                if (chartEntity != null) {
                    CategoryItemEntity cateEntity = (CategoryItemEntity) chartEntity;
                    ICustomerDataset dataEntity = (ICustomerDataset) cateEntity.getDataset();

                    ChartDataEntity currentDataEntity = null;
                    ChartDataEntity[] dataEntities = dataEntity.getDataEntities();
                    if (dataEntities.length == 1) {
                        currentDataEntity = dataEntities[0];
                    } else {
                        for (ChartDataEntity entity : dataEntities) {
                            if (cateEntity.getColumnKey().compareTo(entity.getLabel()) == 0) {
                                currentDataEntity = entity;
                            } else {
                                if (cateEntity.getRowKey().compareTo(entity.getLabel()) == 0) {
                                    currentDataEntity = entity;
                                }
                            }
                        }
                    }

                    PopupMenu menu = new PopupMenu(DefaultMessagesImpl.getString("ColumnAnalysisResultPage.popupMenu")); //$NON-NLS-1$
                    if (currentDataEntity != null) {
                        final Indicator currentIndicator = currentDataEntity.getIndicator();
                        int createPatternFlag = 0;
                        MenuItemEntity[] itemEntities = ChartTableMenuGenerator.generate(explorer, analysis, currentDataEntity);
                        for (final MenuItemEntity itemEntity : itemEntities) {
                            MenuItem item = new MenuItem(itemEntity.getLabel());
                            item.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent arg0) {
                                    Display.getDefault().asyncExec(new Runnable() {

                                        public void run() {
                                            TdDataProvider tdDataProvider = SwitchHelpers.TDDATAPROVIDER_SWITCH.doSwitch(analysis
                                                    .getContext().getConnection());
                                            String query = itemEntity.getQuery();
                                            String editorName = currentIndicator.getName();
                                            CorePlugin.getDefault().runInDQViewer(tdDataProvider, query, editorName);
                                        }

                                    });
                                }

                            });
                            menu.add(item);
                            if ((currentIndicator instanceof PatternFreqIndicator || currentIndicator instanceof PatternLowFreqIndicator)
                                    && createPatternFlag == 0) {
                                MenuItem itemCreatePatt = new MenuItem("Generate Regular Pattern");
                                final PatternTransformer pattTransformer = new PatternTransformer(DbmsLanguageFactory
                                        .createDbmsLanguage(analysis));
                                itemCreatePatt.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        Display.getDefault().asyncExec(new Runnable() {

                                            public void run() {
                                                ChartTableFactory.createPattern(analysis, itemEntity, pattTransformer);
                                            }
                                        });
                                    }
                                });
                                menu.add(itemCreatePatt);
                            }
                        }

                        chartPanel.add(menu);
                        menu.show(chartPanel, event.getTrigger().getX(), event.getTrigger().getY());
                        createPatternFlag++;
                    }
                }
            }

            public void chartMouseMoved(ChartMouseEvent event) {
                // TODO Auto-generated method stub

            }
        });
    }
}
