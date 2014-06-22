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
package org.talend.dq.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.cwm.db.connection.ConnectionUtils;
import org.talend.cwm.helper.ColumnHelper;
import org.talend.cwm.helper.ColumnSetHelper;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.PackageHelper;
import org.talend.cwm.helper.ResourceHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.management.i18n.Messages;
import org.talend.cwm.relational.TdColumn;
import org.talend.dataquality.PluginConstant;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.AnalysisContext;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dq.dbms.GenericSQLHandler;
import org.talend.dq.helper.EObjectHelper;
import org.talend.dq.indicators.IndicatorEvaluator;
import org.talend.utils.sugars.ReturnCode;
import org.talend.utils.sugars.TypedReturnCode;
import orgomg.cwm.objectmodel.core.Classifier;
import orgomg.cwm.objectmodel.core.ModelElement;
import orgomg.cwm.objectmodel.core.Package;
import orgomg.cwm.resource.relational.Catalog;
import orgomg.cwm.resource.relational.ColumnSet;
import orgomg.cwm.resource.relational.Schema;

/**
 * @author scorreia
 * 
 * Run analysis on columns.
 */
public class ColumnAnalysisExecutor extends AnalysisExecutor {

    private Connection dataprovider;

    private static Logger log = Logger.getLogger(ColumnAnalysisExecutor.class);

    protected Map<ModelElement, Package> schemata = new HashMap<ModelElement, Package>();

    protected boolean isAccessWith(Connection dp) {
        if (dataprovider == null) {
            dataprovider = dp;
            return true;
        }
        // else compare
        return ResourceHelper.areSame(dataprovider, dp);
    }

    protected boolean runAnalysis(Analysis analysis, String sqlStatement) {
        IndicatorEvaluator eval = new IndicatorEvaluator(analysis);
        // MOD xqliu 2009-02-09 bug 6237
        eval.setMonitor(getMonitor());
        // --- add indicators
        EList<Indicator> indicators = analysis.getResults().getIndicators();
        for (Indicator indicator : indicators) {
            assert indicator != null;
            TdColumn tdColumn = SwitchHelpers.COLUMN_SWITCH.doSwitch(indicator.getAnalyzedElement());
            if (tdColumn == null) {
                continue;
            }
            // --- get the schema owner
            if (!belongToSameSchemata(tdColumn)) {
                this.errorMessage = Messages.getString("ColumnAnalysisExecutor.GivenColumn", tdColumn.getName()); //$NON-NLS-1$
                return false;
            }
            String columnName = ColumnHelper.getFullName(tdColumn);
            eval.storeIndicator(columnName, indicator);
        }

        // get the dataprovider of the analysis
        org.talend.core.model.metadata.builder.connection.Connection analysisDataProvider = getAnalysisDataProvider(analysis);
        // reset the connection pool before run this analysis
        resetConnectionPool(analysis, analysisDataProvider);

        // open a connection
        TypedReturnCode<java.sql.Connection> connection = null;
        if (POOLED_CONNECTION) {
            connection = getPooledConnection(analysis, analysisDataProvider);
        } else {
            connection = getConnection(analysis);
        }

        if (!connection.isOk()) {
            log.error(connection.getMessage());
            this.errorMessage = connection.getMessage();
            return false;
        }

        // set it into the evaluator
        eval.setConnection(connection.getObject());
        // use pooled connection
        eval.setPooledConnection(POOLED_CONNECTION);

        // when to close connection
        boolean closeAtTheEnd = true;
        Package catalog = schemata.values().iterator().next();
        if (!eval.selectCatalog(catalog.getName())) {
            log.warn(Messages.getString("ColumnAnalysisExecutor.FAILEDTOSELECTCATALOG", catalog.getName()));//$NON-NLS-1$
        }
        ReturnCode rc = eval.evaluateIndicators(sqlStatement, closeAtTheEnd);

        // MOD gdbu 2011-8-15 : file delimited connection is null
        if (POOLED_CONNECTION && null != connection) {
            // release the pooled connection
            releasePooledConnection(analysis, analysisDataProvider, connection.getObject(), true);
        } else {
            ConnectionUtils.closeConnection(connection.getObject());
        }

        if (!rc.isOk()) {
            log.warn(rc.getMessage());
            this.errorMessage = rc.getMessage();
        }
        return rc.isOk();
    }

    /**
     * Method "belongToSameSchemata" fills in the map this{@link #schemata}.
     * 
     * @param tdColumn a column
     * @return false when the given column has an owner different from the one registered in the map.
     */
    protected boolean belongToSameSchemata(final TdColumn tdColumn) {
        assert tdColumn != null;
        if (schemata.get(tdColumn) != null) {
            return true;
        }
        // get table or view
        ColumnSet owner = ColumnHelper.getColumnOwnerAsColumnSet(tdColumn);
        if (owner == null) {
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.NotFoundColumn", tdColumn.getName()); //$NON-NLS-1$
            return false;
        }
        // get catalog or schema
        Package schema = ColumnSetHelper.getParentCatalogOrSchema(owner);
        if (schema == null) {
            this.errorMessage = Messages.getString(
                    "ColumnAnalysisExecutor.NoSchemaOrCatalogFound", owner.getName(), tdColumn.getName()); //$NON-NLS-1$
            return false;
        }

        schemata.put(tdColumn, schema);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.analysis.AnalysisExecutor#createSqlStatement(org.talend.dataquality.analysis.Analysis)
     */
    @Override
    protected String createSqlStatement(Analysis analysis) {
        this.cachedAnalysis = analysis;
        // CwmZQuery query = new CwmZQuery();
        StringBuilder sql = new StringBuilder("SELECT ");//$NON-NLS-1$
        EList<ModelElement> analysedElements = analysis.getContext().getAnalysedElements();
        if (analysedElements.isEmpty()) {
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.CannotCreateSQLStatement",//$NON-NLS-1$
                    analysis.getName());
            return null;
        }
        Set<ColumnSet> fromPart = new HashSet<ColumnSet>();
        final Iterator<ModelElement> iterator = analysedElements.iterator();
        while (iterator.hasNext()) { // for (ModelElement modelElement : analysedElements) {
            ModelElement modelElement = iterator.next();
            // --- preconditions
            TdColumn col = SwitchHelpers.COLUMN_SWITCH.doSwitch(modelElement);
            if (col == null) {
                this.errorMessage = Messages.getString("ColumnAnalysisExecutor.GivenElementIsNotColumn", modelElement); //$NON-NLS-1$
                return null;
            }
            Classifier owner = col.getOwner();
            if (owner == null) {
                this.errorMessage = Messages.getString("ColumnAnalysisExecutor.NoOwnerFound", col.getName()); //$NON-NLS-1$
            }
            ColumnSet colSet = SwitchHelpers.COLUMN_SET_SWITCH.doSwitch(owner);
            if (colSet == null) {
                // FIXME colSet is Null.
                this.errorMessage = Messages.getString("ColumnAnalysisExecutor.NoContainerFound", col.getName(), colSet); //$NON-NLS-1$
                return null;
            }
            // else add into select
            // MOD zshen feature 12919 select all the column to be prepare for drill down when user need.
            if (!analysis.getParameters().isStoreData()) {
                sql.append(this.quote(col.getName()));
                // append comma if more columns exist
                if (iterator.hasNext()) {
                    sql.append(',');//$NON-NLS-1$
                }
            }

            // if (!query.addSelect(col)) {
            //                this.errorMessage = Messages.getString("ColumnAnalysisExecutor.Problem"); //$NON-NLS-1$
            // return null;
            // }
            // add from
            fromPart.add(colSet);

        }
        if (fromPart.size() != 1) {
            log.error(Messages.getString("ColumnAnalysisExecutor.ANALYSISMUSTRUNONONETABLE") + fromPart.size() + PluginConstant.DOT_STRING);//$NON-NLS-1$
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.ANALYSISMUSTRUNONONETABLEERRORMESSAGE");//$NON-NLS-1$
            return null;
        }
        // MOD zshen feature 12919 select all the column to be prepare for drill down.
        if (analysis.getParameters().isStoreData()) {
            // MOD klliu 2011-06-30 bug 22523 whichever is Table or View,that finds columns should ues columnset
            EObject eContainer = analysedElements.get(0).eContainer();
            List<TdColumn> columnList = ColumnSetHelper.getColumns(SwitchHelpers.COLUMN_SET_SWITCH.doSwitch(eContainer));
            // ~
            Iterator<TdColumn> iter = columnList.iterator();
            while (iter.hasNext()) {
                TdColumn column = iter.next();
                sql.append(this.quote(column.getName()));
                // append comma if more columns exist
                if (iter.hasNext()) {
                    sql.append(',');
                }
            }
        }

        // add from clause
        sql.append(dbms().from());
        // if(CatalogHelper.fromPart.iterator().next())
        ModelElement element = fromPart.iterator().next();
        Package parentRelation = PackageHelper.getParentPackage((MetadataTable) fromPart.iterator().next());
        if (parentRelation instanceof Schema) {
            sql.append(dbms().toQualifiedName(null, parentRelation.getName(), element.getName()));
        } else if (parentRelation instanceof Catalog) {
            String ownerUser = null;
            if (ConnectionUtils.isSybaseeDBProducts(dbms().getDbmsName())) {
                ownerUser = ColumnSetHelper.getTableOwner((ModelElement) element);
            }
            sql.append(dbms().toQualifiedName(parentRelation.getName(), ownerUser, element.getName()));
        }
        // String catalog = SwitchHelpers.CATALOG_SWITCH.doSwitch(element);

        // sql.append(this.quote(TableHelper.getParentCatalogOrSchema(fromPart.iterator().next()).getName()));
        // sql.append(".");
        // sql.append(this.quote(fromPart.iterator().next().getName()));

        // add where clause
        // --- get data filter
        ModelElementAnalysisHandler handler = new ModelElementAnalysisHandler();
        handler.setAnalysis(analysis);
        String stringDataFilter = handler.getStringDataFilter();

        sql.append(GenericSQLHandler.WHERE_CLAUSE);

        String sqlStatement = sql.toString();
        sqlStatement = dbms().addWhereToStatement(sqlStatement, stringDataFilter);
        return sqlStatement;
    }

    @Override
    protected boolean check(final Analysis analysis) {
        if (analysis == null) {
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.AnalysisIsNull"); //$NON-NLS-1$
            return false;
        }
        if (!super.check(analysis)) {
            // error message already set in super method.
            return false;
        }

        // --- check existence of context
        AnalysisContext context = analysis.getContext();
        if (context == null) {
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.NoContextSet", analysis.getName()); //$NON-NLS-1$
            return false;
        }

        // --- check that there exists at least on element to analyze
        if (context.getAnalysedElements().size() == 0) {
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.AnalysisHaveAtLeastOneColumn"); //$NON-NLS-1$
            return false;
        }

        // --- check that the connection has been set
        if (context.getConnection() == null) {
            this.errorMessage = Messages.getString("ColumnAnalysisExecutor.NoConnectionSet"); //$NON-NLS-1$
            return false;
        }

        return checkAnalyzedElements(analysis, context);
    }

    /**
     * DOC scorreia Comment method "checkAnalyzedElements".
     * 
     * @param analysis
     * @param context
     */
    protected boolean checkAnalyzedElements(final Analysis analysis, AnalysisContext context) {
        ModelElementAnalysisHandler analysisHandler = new ModelElementAnalysisHandler();
        analysisHandler.setAnalysis(analysis);

        for (ModelElement node : context.getAnalysedElements()) {
            TdColumn column = SwitchHelpers.COLUMN_SWITCH.doSwitch(node);

            // --- Check that each analyzed element has at least one indicator
            if (analysisHandler.getIndicators(column).size() == 0) {
                this.errorMessage = Messages.getString("ColumnAnalysisExecutor.EachColumnHaveOneIndicator"); //$NON-NLS-1$
                return false;
            }

            // --- get the data provider
            Connection dp = ConnectionHelper.getTdDataProvider(column);
            if (!isAccessWith(dp)) {
                this.errorMessage = Messages.getString("ColumnAnalysisExecutor.AllColumnsBelongSameConnection", //$NON-NLS-1$
                        column.getName(), dataprovider.getName());
                return false;
            }
        }
        return true;
    }

    /**
     * Method "getQuotedColumnName".
     * 
     * @param column a column
     * @return the quoted column name
     */
    protected String getQuotedColumnName(ModelElement column) {
        if (column != null && column.eIsProxy()) {
            column = (ModelElement) EObjectHelper.resolveObject(column);
        }
        assert column != null;
        String quotedColName = quote(column.getName());
        return quotedColName;
    }

    /**
     * Method "getQuotedTableName".
     * 
     * @param column
     * @return the quoted table name
     */
    protected String getQuotedTableName(TdColumn column) {
        if (column != null && column.eIsProxy()) {
            column = (TdColumn) EObjectHelper.resolveObject(column);
        }
        String table = quote(ColumnHelper.getTableFullName(column));
        return table;
    }

    /**
     * DOC xqliu Comment method "isOracle".
     * 
     * @return
     */
    protected boolean isOracle() {
        return ConnectionHelper.isOracle(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isMysql".
     * 
     * @return
     */
    protected boolean isMysql() {
        return ConnectionHelper.isMysql(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isMssql".
     * 
     * @return
     */
    protected boolean isMssql() {
        return ConnectionHelper.isMssql(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isPostgresql".
     * 
     * @return
     */
    protected boolean isPostgresql() {
        return ConnectionHelper.isPostgresql(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isInformix".
     * 
     * @return
     */
    protected boolean isInformix() {
        return ConnectionHelper.isInformix(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isIngress".
     * 
     * @return
     */
    protected boolean isIngress() {
        return ConnectionHelper.isIngress(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isDb2".
     * 
     * @return
     */
    protected boolean isDb2() {
        return ConnectionHelper.isDb2(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isSybase".
     * 
     * @return
     */
    protected boolean isSybase() {
        return ConnectionHelper.isSybase(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isTeradata".
     * 
     * @return
     */
    protected boolean isTeradata() {
        return ConnectionHelper.isTeradata(dataprovider);
    }

    /**
     * DOC xqliu Comment method "isNetezza".
     * 
     * @return
     */
    protected boolean isNetezza() {
        return ConnectionHelper.isNetezza(dataprovider);
    }
}