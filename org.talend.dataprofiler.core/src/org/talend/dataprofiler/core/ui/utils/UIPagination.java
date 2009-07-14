// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.ui.editor.analysis.PaginationInfo;
import org.talend.dataprofiler.core.ui.pref.EditorPreferencePage;

/**
 * 
 * DOC mzhao 2009-04-20,UI pagination.
 */
public class UIPagination {

    private static Logger log = Logger.getLogger(UIPagination.class);

    private static final int PAGE_SIZE = 5;

    private int totalPages;

    private int currentPage;

    private List<PaginationInfo> pageCache = new ArrayList<PaginationInfo>();

    private ImageHyperlink pageLastImgHypLnk = null;

    private ImageHyperlink pageNextImgHypLnk = null;

    private ImageHyperlink pagePreviouseImgHypLnk = null;

    private ImageHyperlink pageFirstImgHypLnk = null;

    private ImageHyperlink goImgHypLnk = null;

    private Text pageGoText;

    private FormToolkit toolkit;

    private Composite composite;

    private Label pageInfoLabel;

    private Composite pageNavComp;

    public UIPagination(FormToolkit toolkit, Composite composite) {
        this.toolkit = toolkit;
        this.composite = composite;
        currentPage = 0;
        totalPages = 0;

    }

    public void init() {
        createNavComposite(composite);
        initPageNav();
        notifyPageNavigator();
        // First show zero-indexed contents.
        if (pageCache.size() > 0) {
            pageCache.get(0).renderContents();
        }

    }

    public void pack() {
        composite.layout();
        composite.pack();
        pageNavComp.layout();
        // pageNavComp.pack();
    }

    public FormToolkit getToolkit() {
        return toolkit;
    }

    public Composite getComposite() {
        return composite;
    }

    public void updatePageInfoLabel() {
        pageInfoLabel.setText(currentPage + 1 + "/" + totalPages);
    }

    private void createNavComposite(Composite searchMainComp) {
        pageNavComp = toolkit.createComposite(searchMainComp, SWT.NONE);
        final GridData pageNavCompGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
        pageNavCompGD.heightHint = 25;
        pageNavCompGD.minimumWidth = 0;
        pageNavComp.setLayoutData(pageNavCompGD);
        pageNavComp.setLayout(new FormLayout());
        toolkit.paintBordersFor(pageNavComp);

        pageInfoLabel = toolkit.createLabel(pageNavComp, "", SWT.NONE);
        final FormData fdLabel = new FormData();
        fdLabel.bottom = new FormAttachment(100, 0);
        fdLabel.top = new FormAttachment(0, 5);
        fdLabel.right = new FormAttachment(100, -10);
        pageInfoLabel.setLayoutData(fdLabel);

        pageLastImgHypLnk = toolkit.createImageHyperlink(pageNavComp, SWT.NONE);
        final FormData fdImageHyperlink = new FormData();
        fdImageHyperlink.right = new FormAttachment(pageInfoLabel, -20, SWT.LEFT);
        fdImageHyperlink.bottom = new FormAttachment(pageInfoLabel, -3, SWT.BOTTOM);
        fdImageHyperlink.top = new FormAttachment(pageInfoLabel, 0, SWT.TOP);
        pageLastImgHypLnk.setLayoutData(fdImageHyperlink);

        pageNextImgHypLnk = toolkit.createImageHyperlink(pageNavComp, SWT.NONE);
        final FormData pgNextImgFD = new FormData();
        pgNextImgFD.right = new FormAttachment(pageLastImgHypLnk, -10, SWT.LEFT);
        pgNextImgFD.bottom = new FormAttachment(pageLastImgHypLnk, 0, SWT.BOTTOM);
        pgNextImgFD.top = new FormAttachment(pageLastImgHypLnk, 0, SWT.TOP);
        pageNextImgHypLnk.setLayoutData(pgNextImgFD);

        pagePreviouseImgHypLnk = toolkit.createImageHyperlink(pageNavComp, SWT.NONE);
        final FormData pgPreImgFD = new FormData();
        pgPreImgFD.right = new FormAttachment(pageNextImgHypLnk, -10, SWT.LEFT);
        pgPreImgFD.bottom = new FormAttachment(pageNextImgHypLnk, 0, SWT.BOTTOM);
        pgPreImgFD.top = new FormAttachment(pageNextImgHypLnk, 0, SWT.TOP);
        pagePreviouseImgHypLnk.setLayoutData(pgPreImgFD);

        pageFirstImgHypLnk = toolkit.createImageHyperlink(pageNavComp, SWT.NONE);
        final FormData pgFirImgFD = new FormData();
        pgFirImgFD.right = new FormAttachment(pagePreviouseImgHypLnk, -10, SWT.LEFT);
        pgFirImgFD.bottom = new FormAttachment(pagePreviouseImgHypLnk, 0, SWT.BOTTOM);
        pgFirImgFD.top = new FormAttachment(pagePreviouseImgHypLnk, 0, SWT.TOP);
        pageFirstImgHypLnk.setLayoutData(pgFirImgFD);

        pageGoText = toolkit.createText(pageNavComp, null, SWT.NONE);

        final FormData tdText = new FormData();
        tdText.right = new FormAttachment(pageFirstImgHypLnk, -15, SWT.LEFT);
        tdText.bottom = new FormAttachment(pageFirstImgHypLnk, 0, SWT.BOTTOM);
        tdText.top = new FormAttachment(pageFirstImgHypLnk, 0, SWT.TOP);
        tdText.width = 50;
        pageGoText.setLayoutData(tdText);
        pageGoText.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    e.doit = false;
                    go();
                }
            }

            public void keyReleased(KeyEvent e) {

            }

        });
        goImgHypLnk = toolkit.createImageHyperlink(pageNavComp, SWT.NONE);
        final FormData goImgFD = new FormData();
        goImgFD.right = new FormAttachment(pageGoText, -5, SWT.LEFT);
        goImgFD.bottom = new FormAttachment(pageGoText, 0, SWT.BOTTOM);
        goImgFD.top = new FormAttachment(pageGoText, 0, SWT.TOP);
        goImgHypLnk.setLayoutData(goImgFD);
        goImgHypLnk.setText("Go");
    }

    public static boolean isNumeric(String originStr) {
        originStr = originStr.trim();
        if (originStr.equals("")) {
            return false;
        }
        boolean valideChar = true;
        for (int i = 0; i < originStr.length(); i++) {
            char sigalChar = originStr.charAt(i);
            if (sigalChar < '0' || sigalChar > '9') {
                return false;
            }
        }

        return valideChar;
    }

    public void addPage(PaginationInfo pageInf) {
        pageCache.add(pageInf);
        totalPages++;
    }

    private void initPageNav() {
        pageFirstImgHypLnk.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
                pageCache.get(currentPage).dispose();
                currentPage = 0;
                pageCache.get(currentPage).renderContents();
            }

        });
        pagePreviouseImgHypLnk.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
                pageCache.get(currentPage).dispose();
                currentPage = currentPage - 1;
                pageCache.get(currentPage).renderContents();
            }
        });
        pageNextImgHypLnk.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
                pageCache.get(currentPage).dispose();
                currentPage = currentPage + 1;
                pageCache.get(currentPage).renderContents();
            }

        });
        pageLastImgHypLnk.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
                pageCache.get(currentPage).dispose();
                currentPage = totalPages - 1;
                pageCache.get(currentPage).renderContents();
            }
        });
        goImgHypLnk.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
                go();
            }
        });

    }

    private void go() {

        if (!isNumeric(pageGoText.getText().trim())) {
            MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error",
                    "Page number should be a valid integer.");
            return;
        }
        Integer goNo = null;
        try {
            goNo = Integer.parseInt(pageGoText.getText().trim());
        } catch (Exception exc) {
            MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error",
                    "Page number not in an valid range.");
            return;
        }
        if (goNo < 1 || goNo > totalPages) {
            MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error",
                    "Page number not in an valid range.");
            return;
        }
        pageCache.get(currentPage).dispose();
        currentPage = goNo - 1;
        pageCache.get(currentPage).renderContents();

    }

    public void notifyPageNavigator() {
        if (totalPages == 0) {
            setNavImgState(pageFirstImgHypLnk, IMG_LNK_NAV_FIRST, false);
            setNavImgState(pagePreviouseImgHypLnk, IMG_LNK_NAV_PREV, false);
            setNavImgState(pageLastImgHypLnk, IMG_LNK_NAV_LAST, false);
            setNavImgState(pageNextImgHypLnk, IMG_LNK_NAV_NEXT, false);
            goImgHypLnk.setEnabled(false);
            return;
        }
        if (currentPage > 0) {
            setNavImgState(pageFirstImgHypLnk, IMG_LNK_NAV_FIRST, true);
            setNavImgState(pagePreviouseImgHypLnk, IMG_LNK_NAV_PREV, true);
        } else {
            setNavImgState(pageFirstImgHypLnk, IMG_LNK_NAV_FIRST, false);
            setNavImgState(pagePreviouseImgHypLnk, IMG_LNK_NAV_PREV, false);
        }
        if (currentPage < totalPages - 1) {
            setNavImgState(pageLastImgHypLnk, IMG_LNK_NAV_LAST, true);
            setNavImgState(pageNextImgHypLnk, IMG_LNK_NAV_NEXT, true);
        } else {
            setNavImgState(pageLastImgHypLnk, IMG_LNK_NAV_LAST, false);
            setNavImgState(pageNextImgHypLnk, IMG_LNK_NAV_NEXT, false);
        }
        goImgHypLnk.setEnabled(true);
    }

    private void setNavImgState(ImageHyperlink imgHypLnk, Image img, Boolean isEnabled) {
        imgHypLnk.setEnabled(isEnabled);
        imgHypLnk.setImage(getImage(null, img, isEnabled));
    }

    public static Image getImage(Display disp, Image img, Boolean isEnabled) {
        int imgStatus = SWT.IMAGE_DISABLE;
        if (isEnabled) {
            imgStatus = SWT.IMAGE_COPY;
        }
        Image disabledImg = new Image(disp, img, imgStatus);
        return disabledImg;
    }

    private static final Image IMG_LNK_NAV_LAST = ImageLib.getImage(ImageLib.ICON_PAGE_LAST_LNK);

    private static final Image IMG_LNK_NAV_NEXT = ImageLib.getImage(ImageLib.ICON_PAGE_NEXT_LNK);

    private static final Image IMG_LNK_NAV_PREV = ImageLib.getImage(ImageLib.ICON_PAGE_PREV_LNK);

    private static final Image IMG_LNK_NAV_FIRST = ImageLib.getImage(ImageLib.ICON_PAGE_FIRST_LNK);

    public static int getPageSize() {
        try {
            String defaultPageSize = ResourcesPlugin.getPlugin().getPluginPreferences().getString(
                    EditorPreferencePage.ANALYZED_ITEMS_PER_PAGE);
            if (!"".equals(defaultPageSize)) {
                return Integer.parseInt(defaultPageSize);
            }
        } catch (NumberFormatException e) {
            log.error(e, e);
        }
        return PAGE_SIZE;
    }
}
