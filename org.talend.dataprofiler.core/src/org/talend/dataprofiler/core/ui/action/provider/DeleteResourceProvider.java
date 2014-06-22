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
package org.talend.dataprofiler.core.ui.action.provider;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreeSelection;
import org.talend.core.model.properties.Item;
import org.talend.dataprofiler.core.ui.action.actions.DQDeleteAction;
import org.talend.dataprofiler.core.ui.action.actions.RenameTdqFolderAction;
import org.talend.dataprofiler.core.ui.exchange.ExchangeCategoryRepNode;
import org.talend.dataprofiler.core.ui.exchange.ExchangeComponentRepNode;
import org.talend.dataprofiler.core.ui.utils.WorkbenchUtils;
import org.talend.dataquality.properties.TDQIndicatorDefinitionItem;
import org.talend.dq.nodes.AnalysisSubFolderRepNode;
import org.talend.dq.nodes.DBCatalogRepNode;
import org.talend.dq.nodes.DBColumnFolderRepNode;
import org.talend.dq.nodes.DBColumnRepNode;
import org.talend.dq.nodes.DBConnectionSubFolderRepNode;
import org.talend.dq.nodes.DBSchemaRepNode;
import org.talend.dq.nodes.DBTableFolderRepNode;
import org.talend.dq.nodes.DBTableRepNode;
import org.talend.dq.nodes.DBViewFolderRepNode;
import org.talend.dq.nodes.DBViewRepNode;
import org.talend.dq.nodes.DFColumnFolderRepNode;
import org.talend.dq.nodes.DFColumnRepNode;
import org.talend.dq.nodes.DFConnectionSubFolderRepNode;
import org.talend.dq.nodes.DFTableRepNode;
import org.talend.dq.nodes.MDMConnectionSubFolderRepNode;
import org.talend.dq.nodes.MDMSchemaRepNode;
import org.talend.dq.nodes.MDMXmlElementRepNode;
import org.talend.dq.nodes.PatternRegexSubFolderRepNode;
import org.talend.dq.nodes.PatternSqlSubFolderRepNode;
import org.talend.dq.nodes.ReportAnalysisRepNode;
import org.talend.dq.nodes.ReportFileRepNode;
import org.talend.dq.nodes.ReportSubFolderRepNode;
import org.talend.dq.nodes.ReportSubFolderRepNode.ReportSubFolderType;
import org.talend.dq.nodes.RulesParserSubFolderRepNode;
import org.talend.dq.nodes.RulesSQLSubFolderRepNode;
import org.talend.dq.nodes.UserDefIndicatorSubFolderRepNode;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode;
import org.talend.resource.ResourceManager;
import org.talend.resource.ResourceService;

/**
 * DOC rli class global comment. Detailled comment
 */
public class DeleteResourceProvider extends AbstractCommonActionProvider {

    /**
     * Adds a submenu to the given menu with the name "New Component".
     */
    public void fillContextMenu(IMenuManager menu) {
        // MOD mzhao user readonly role on svn repository mode.
        if (!isShowMenu()) {
            return;
        }
        Object obj = ((TreeSelection) this.getContext().getSelection()).getFirstElement();
        if (obj instanceof RepositoryNode) {
            RepositoryNode node = (RepositoryNode) obj;
            // RepositoryNode parent = node.getParent();
            // if (!(parent instanceof ReportSubFolderRepNode)) {
            if (shouldShowDeleteMenu(node)) {
                // menu.add(new DeleteObjectsAction());
                menu.add(new DQDeleteAction());
                if (shouldShowRenameFolderMenu(node)) {
                    menu.add(new RenameTdqFolderAction(node));
                }
            }
            // }
        }
    }

    private boolean shouldShowRenameFolderMenu(RepositoryNode node) {
        boolean show = false;
        if (node instanceof AnalysisSubFolderRepNode) {
            AnalysisSubFolderRepNode anaSubFolderNode = (AnalysisSubFolderRepNode) node;
            show = !anaSubFolderNode.isVirtualFolder();
        } else if (node instanceof ReportSubFolderRepNode) {
            ReportSubFolderRepNode repSubFolderNode = (ReportSubFolderRepNode) node;
            show = !repSubFolderNode.isVirtualFolder();
        } else if (node instanceof UserDefIndicatorSubFolderRepNode || node instanceof PatternRegexSubFolderRepNode
                || node instanceof PatternSqlSubFolderRepNode || node instanceof RulesSQLSubFolderRepNode
                || node instanceof RulesParserSubFolderRepNode || node instanceof DBConnectionSubFolderRepNode
                || node instanceof MDMConnectionSubFolderRepNode || node instanceof DFConnectionSubFolderRepNode) {
            show = true;
        }
        return show;
    }

    private boolean shouldShowDeleteMenu(RepositoryNode node) {
        return (!isSystemFolder(node) && !isVirturalNode(node) && !isSystemIndicator(node) && !node.isBin())
                || (node instanceof ReportFileRepNode);
    }

    private boolean isSystemFolder(RepositoryNode node) {
        return ENodeType.SYSTEM_FOLDER.equals(node.getType());
    }

    private boolean isVirturalNode(RepositoryNode node) {
        return node instanceof DBCatalogRepNode || node instanceof DBSchemaRepNode || node instanceof DBTableFolderRepNode
                || node instanceof DBViewFolderRepNode || node instanceof DBTableRepNode || node instanceof DBViewRepNode
                || node instanceof DBColumnFolderRepNode || node instanceof DBColumnRepNode || node instanceof MDMSchemaRepNode
                || node instanceof MDMXmlElementRepNode || node instanceof DFTableRepNode
                || node instanceof DFColumnFolderRepNode || node instanceof DFColumnRepNode
                || node instanceof ExchangeCategoryRepNode || node instanceof ExchangeComponentRepNode
                || isReportSubFolderVirtualNode(node) || isAnalysisSubFolderVirtualNode(node)
                || node instanceof ReportAnalysisRepNode || node instanceof ReportFileRepNode;
    }

    private boolean isReportSubFolderVirtualNode(RepositoryNode node) {
        if (node instanceof ReportSubFolderRepNode) {
            ReportSubFolderRepNode subFolderNode = (ReportSubFolderRepNode) node;
            return ReportSubFolderType.ANALYSIS.equals(subFolderNode.getReportSubFolderType())
                    || ReportSubFolderType.GENERATED_DOCS.equals(subFolderNode.getReportSubFolderType());
        }
        return false;
    }

    private boolean isAnalysisSubFolderVirtualNode(RepositoryNode node) {
        if (node instanceof AnalysisSubFolderRepNode) {
            AnalysisSubFolderRepNode subFolderNode = (AnalysisSubFolderRepNode) node;
            return subFolderNode.getObject() == null;
        }
        return false;
    }

    private boolean isSystemIndicator(RepositoryNode node) {
        switch (node.getType()) {
        case TDQ_REPOSITORY_ELEMENT:
        case REPOSITORY_ELEMENT:
            if (node.getObject() != null) {
                Item item = node.getObject().getProperty().getItem();
                IFolder folder = WorkbenchUtils.getFolder(node);
                return item instanceof TDQIndicatorDefinitionItem
                        && ResourceService.isSubFolder(ResourceManager.getSystemIndicatorFolder(), folder);
            }
        default:

        }
        return false;
    }

}