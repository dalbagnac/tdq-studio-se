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
package org.talend.dq.nodes;

import java.util.ArrayList;
import java.util.List;

import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.repositoryObject.TdTableRepositoryObject;
import org.talend.cwm.relational.TdTable;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC klliu Database table repository node displayed on repository view (UI).
 */
public class DBTableRepNode extends DQRepositoryNode {

    private TdTableRepositoryObject tdTableRepositoryObject;

    private TdTable tdTable;

    private List<IRepositoryNode> casheChildren;

    public TdTableRepositoryObject getTdTableRepositoryObject() {
        return this.tdTableRepositoryObject;
    }

    public TdTable getTdTable() {
        return this.tdTable;
    }

    /**
     * DOC klliu DBTableRepNode constructor comment.
     * 
     * @param object
     * @param parent
     * @param type
     */
    public DBTableRepNode(IRepositoryViewObject object, RepositoryNode parent, ENodeType type) {
        super(object, parent, type);
        if (object instanceof TdTableRepositoryObject) {
            this.tdTableRepositoryObject = (TdTableRepositoryObject) object;
            this.tdTable = this.tdTableRepositoryObject.getTdTable();
        }
        casheChildren = new ArrayList<IRepositoryNode>();
    }

    /*
     * (non-Jsdoc)
     * 
     * @see org.talend.repository.model.RepositoryNode#getChildren()
     */
    @Override
    public List<IRepositoryNode> getChildren() {
        // MOD klliu TDQ-4332 only initlized sub node(column folder) once time 2012-02-07
        if (!casheChildren.isEmpty()) {
            return filterResultsIfAny(casheChildren);
        }
        // MOD gdbu 2011-7-1 bug : 22204
        // TdTableRepositoryObject viewObject = ((TdTableRepositoryObject) this.getObject());
        DBColumnFolderRepNode columnFolderNode = new DBColumnFolderRepNode(null, this, ENodeType.TDQ_REPOSITORY_ELEMENT);
        casheChildren.add(columnFolderNode);
        return filterResultsIfAny(casheChildren);
        // ~22204
    }

    /**
     * get the parent package node of this table.
     * 
     * @param node
     * @return
     */
    public static IRepositoryNode getParentPackageNode(IRepositoryNode node) {
        if (node == null) {
            return null;
        }
        if (node instanceof DBSchemaRepNode || node instanceof DBCatalogRepNode) {
            return node;
        } else {
            return getParentPackageNode(node.getParent());
        }
    }

    @Override
    public String getLabel() {
        return this.getTdTable().getName();
    }

    public List<IRepositoryNode> getCasheChildren() {
        return this.casheChildren;
    }

}