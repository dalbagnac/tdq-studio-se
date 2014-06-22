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
import org.talend.core.repository.model.repositoryObject.TdViewRepositoryObject;
import org.talend.cwm.relational.TdView;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC klliu Database view repository node displayed on repository view (UI).
 */
public class DBViewRepNode extends DQRepositoryNode {

    private TdViewRepositoryObject tdViewRepositoryObject;

    private TdView tdView;

    public TdViewRepositoryObject getTdViewRepositoryObject() {
        return this.tdViewRepositoryObject;
    }

    public TdView getTdView() {
        return this.tdView;
    }

    /**
     * DOC klliu DBViewRepNode constructor comment.
     * 
     * @param object
     * @param parent
     * @param type
     */
    public DBViewRepNode(IRepositoryViewObject object, RepositoryNode parent, ENodeType type) {
        super(object, parent, type);
        if (object instanceof TdViewRepositoryObject) {
            this.tdViewRepositoryObject = (TdViewRepositoryObject) object;
            this.tdView = this.tdViewRepositoryObject.getTdView();
        }
    }

    /*
     * (non-Jsdoc)
     * 
     * @see org.talend.repository.model.RepositoryNode#getChildren()
     */
    @Override
    public List<IRepositoryNode> getChildren() {
        //MOD gdbu 2011-7-1 bug : 22204
        List<IRepositoryNode> nodes = new ArrayList<IRepositoryNode>();
        DBColumnFolderRepNode columnFolderNode = new DBColumnFolderRepNode(getObject(), this, ENodeType.TDQ_REPOSITORY_ELEMENT);
        nodes.add(columnFolderNode);
        return filterResultsIfAny(nodes);
        // ~22204
    }

    @Override
    public String getLabel() {
        return this.tdView.getName();
    }
}