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
package org.talend.dataprofiler.core.ui.editor.preview.model.states.pattern;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.TopChartFactory;
import org.talend.dataprofiler.core.ui.editor.preview.ext.PatternMatchingExt;
import org.talend.dataprofiler.core.ui.editor.preview.model.ICustomerDataset;
import org.talend.dataprofiler.core.ui.editor.preview.model.dataset.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.AbstractChartTypeStates;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderFactory.CommonContenteProvider;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderFactory.PatternLabelProvider;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.analysis.explore.PatternExplorer;
import org.talend.dq.indicators.preview.table.PatternChartDataEntity;

/**
 * DOC Zqin class global comment. Detailled comment
 */
public class PatternStatisticsState extends AbstractChartTypeStates {

    public PatternStatisticsState(List<IndicatorUnit> units) {
        super(units);
        // TODO Auto-generated constructor stub
    }

    public JFreeChart getChart() {
        // TODO Auto-generated method stub
        return TopChartFactory.createStacked3DBarChart("Pattern Statistics", getDataset(), PlotOrientation.VERTICAL);
    }

    public ICustomerDataset getCustomerDataset() {
        CustomerDefaultCategoryDataset customerdataset = new CustomerDefaultCategoryDataset();
        for (IndicatorUnit unit : units) {
            String label = unit.getIndicatorName();
            PatternMatchingExt patternExt = (PatternMatchingExt) unit.getValue();
            double notMathCount = patternExt.getNotMatchingValueCount();
            double machCount = patternExt.getMatchingValueCount();

            customerdataset.addValue(machCount, "matching", label); //$NON-NLS-1$
            customerdataset.addValue(notMathCount, "not matching", label); //$NON-NLS-1$

            PatternChartDataEntity patternEntity = new PatternChartDataEntity();
            patternEntity.setIndicator(unit.getIndicator());
            patternEntity.setLabel(unit.getIndicatorName());
            patternEntity.setNumMatch(String.valueOf(machCount));
            patternEntity.setNumNoMatch(String.valueOf(notMathCount));

            customerdataset.addDataEntity(patternEntity);
        }

        return customerdataset;
    }

    public DataExplorer getDataExplorer() {
        // TODO Auto-generated method stub
        return new PatternExplorer();
    }

    public JFreeChart getExampleChart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected TableStructureEntity getTableStructure() {
        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] { "Label", "%Match", "%No Match", "#Match", "#No Match" });
        entity.setFieldWidths(new Integer[] { 200, 75, 75, 75, 75 });
        return entity;
    }

    @Override
    protected ITableLabelProvider getLabelProvider() {
        // TODO Auto-generated method stub
        return new PatternLabelProvider();
    }

    @Override
    protected IStructuredContentProvider getContentProvider() {
        // TODO Auto-generated method stub
        return new CommonContenteProvider();
    }
}
