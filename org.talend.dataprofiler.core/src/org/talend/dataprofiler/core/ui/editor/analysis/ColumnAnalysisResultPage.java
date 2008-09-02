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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.ui.internal.browser.WebBrowserEditorInput;
import org.talend.cwm.relational.TdColumn;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.exception.ExceptionHandler;
import org.talend.dataprofiler.core.model.ColumnIndicator;
import org.talend.dataprofiler.core.ui.editor.AbstractFormPage;
import org.talend.dataprofiler.core.ui.editor.preview.CompositeIndicator;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorChartFactory;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableFactory;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartWithData;
import org.talend.dq.analysis.ColumnAnalysisHandler;

/**
 * DOC zqin class global comment. Detailled comment
 */
public class ColumnAnalysisResultPage extends AbstractFormPage implements PropertyChangeListener {

    private Composite summaryComp;

    private Composite resultComp;

    private ScrolledForm form;

    private ColumnMasterDetailsPage masterPage;

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
        this.form = managedForm.getForm();
        this.form.setText("Analysis Result");
        Composite body = form.getBody();
        body.setLayout(new GridLayout());

        summaryComp = toolkit.createComposite(body);
        summaryComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        summaryComp.setLayout(new GridLayout());
        createSummarySection(summaryComp);

        resultComp = toolkit.createComposite(body);
        resultComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        resultComp.setLayout(new GridLayout());
        createResultSection(resultComp);

        form.reflow(true);
    }

    private void createSummarySection(Composite parent) {
        Section section = createSection(form, parent, "Analysis Summary", true, null);
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new GridLayout(2, false));
        sectionClient.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite databaseComp = toolkit.createComposite(sectionClient);
        databaseComp.setLayout(new GridLayout(2, false));
        GridData databaseCompData = new GridData(GridData.FILL_HORIZONTAL);
        databaseCompData.verticalAlignment = GridData.BEGINNING;
        databaseComp.setLayoutData(databaseCompData);

        ColumnAnalysisHandler handler = this.masterPage.getAnalysisHandler();
        toolkit.createLabel(databaseComp, "Connection:");
        toolkit.createLabel(databaseComp, handler.getConnectionName());
        if (handler.isCatalogExisting()) {
            toolkit.createLabel(databaseComp, "Catalog:");
            toolkit.createLabel(databaseComp, handler.getCatalogNames());
        }

        if (handler.isSchemaExisting()) {
            toolkit.createLabel(databaseComp, "Schema:");
            toolkit.createLabel(databaseComp, handler.getSchemaNames());
        }

        toolkit.createLabel(databaseComp, "Table(s):");
        toolkit.createLabel(databaseComp, handler.getTableNames());

        Composite executionComp = toolkit.createComposite(sectionClient);
        executionComp.setLayout(new GridLayout(2, false));
        GridData executionCompData = new GridData(GridData.FILL_HORIZONTAL);
        executionCompData.verticalAlignment = GridData.BEGINNING;
        executionComp.setLayoutData(executionCompData);
        toolkit.createLabel(executionComp, "Execution Date:");
        toolkit.createLabel(executionComp, handler.getExecuteData());
        toolkit.createLabel(executionComp, "Execution Duration:");
        toolkit.createLabel(executionComp, handler.getExecuteDuration());
        toolkit.createLabel(executionComp, "Execution Status:");
        if (handler.getResultMetadata().isLastRunOk()) {
            toolkit.createLabel(executionComp, "success");
        } else {
            toolkit.createLabel(executionComp, "failure:" + handler.getResultMetadata().getMessage()).setForeground(
                    Display.getDefault().getSystemColor(SWT.COLOR_RED));
        }

        toolkit.createLabel(executionComp, "Number of Execution:");
        toolkit.createLabel(executionComp, handler.getExecuteNumber());
        toolkit.createLabel(executionComp, "Last Sucessful Execution:");
        toolkit.createLabel(executionComp, handler.getLastExecutionNumberOk());

        section.setClient(sectionClient);
    }

    private void createResultSection(Composite parent) {
        Section section = createSection(form, parent, "Analysis Results", true, null);
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new GridLayout());
        sectionClient.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (final ColumnIndicator columnIndicator : masterPage.getTreeViewer().getColumnIndicator()) {

            ExpandableComposite exComp = toolkit.createExpandableComposite(sectionClient, ExpandableComposite.TWISTIE
                    | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.EXPANDED);
            exComp.setText("Column: " + columnIndicator.getTdColumn().getName());
            exComp.setLayout(new GridLayout());
            exComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

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

                    monitor.beginTask("Creating preview for " + column.getName(), IProgressMonitor.UNKNOWN);

                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {

                            for (ChartWithData chartData : IndicatorChartFactory.createChart(columnIndicator, true)) {

                                ExpandableComposite subComp = toolkit.createExpandableComposite(comp, ExpandableComposite.TWISTIE
                                        | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.EXPANDED);
                                subComp.setText(chartData.getChartNamedType());

                                Composite composite = toolkit.createComposite(subComp);
                                composite.setLayout(new GridLayout(2, false));
                                composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

                                // create table
                                ChartTableFactory.createTable(composite, chartData);
                                // carete image
                                ImageDescriptor imageDescriptor = chartData.getImageDescriptor();
                                if (imageDescriptor != null) {
                                    ImageHyperlink image = toolkit.createImageHyperlink(composite, SWT.WRAP);
                                    image.setImage(imageDescriptor.createImage());
                                    if (chartData.getChartNamedType().equals(CompositeIndicator.SUMMARY_STATISTICS)) {
                                        addShowDefinition(image);
                                    }
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
        // TODO Auto-generated method stub

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

    /**
     * DOC qzhang Comment method "addShowDefinition".
     * 
     * @param image
     */
    public static void addShowDefinition(ImageHyperlink image) {
        image.setToolTipText("What is it?");
        image.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                try {
                    WebBrowserEditor.open(new WebBrowserEditorInput(new URL("http://en.wikipedia.org/wiki/Box_plot")));
                } catch (MalformedURLException e1) {
                    ExceptionHandler.process(e1);
                }
            }

        });
    }

}
