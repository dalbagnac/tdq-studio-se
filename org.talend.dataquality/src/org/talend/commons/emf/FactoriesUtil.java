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
package org.talend.commons.emf;

import java.util.ArrayList;
import java.util.List;

import org.talend.cwm.constants.ConstantsFactory;
import org.talend.cwm.relational.RelationalFactory;
import org.talend.cwm.relational.RelationalPackage;
import org.talend.cwm.softwaredeployment.SoftwaredeploymentPackage;
import org.talend.dataquality.analysis.AnalysisFactory;
import org.talend.dataquality.analysis.category.CategoryFactory;
import org.talend.dataquality.analysis.category.CategoryPackage;
import org.talend.dataquality.domain.DomainFactory;
import org.talend.dataquality.domain.DomainPackage;
import org.talend.dataquality.domain.pattern.PatternFactory;
import org.talend.dataquality.domain.pattern.PatternPackage;
import org.talend.dataquality.indicators.IndicatorsFactory;
import org.talend.dataquality.indicators.IndicatorsPackage;
import org.talend.dataquality.indicators.definition.DefinitionFactory;
import org.talend.dataquality.indicators.definition.DefinitionPackage;
import org.talend.dataquality.indicators.sql.IndicatorSqlFactory;
import org.talend.dataquality.indicators.sql.IndicatorSqlPackage;
import org.talend.dataquality.reports.ReportsFactory;
import org.talend.dataquality.reports.ReportsPackage;
import org.talend.dataquality.rules.RulesFactory;
import org.talend.dataquality.rules.RulesPackage;
import orgomg.cwm.foundation.typemapping.TypemappingPackage;
import orgomg.cwm.objectmodel.core.CorePackage;

/**
 * @author scorreia
 * 
 * This class is a utility for CWM and Talend extension Factories initialization. MODSCA 2008-04-03 use
 * Factory.eINSTANCE.getEPackage() instead of FactoryImpl.init() so that implementation packages can be hidden.
 */
public final class FactoriesUtil {

    /**
     * Extension used for the files in which the data provider objects are serialized.
     */
    public static final String PROV = "prv"; //$NON-NLS-1$

    /**
     * Extension used for the files in which the analysis objects are serialized.
     */
    public static final String ANA = "ana"; //$NON-NLS-1$

    /**
     * Extension used for the files in which the catalog or schema objects are serialized.
     */
    public static final String CAT = "cat"; //$NON-NLS-1$

    /**
     * Extension used for the files in which the reports are serialized.
     */
    public static final String REP = "rep"; //$NON-NLS-1$

    /**
     * Extension used for the files in which the sql are serialized.
     */
    public static final String SQL = "sql"; //$NON-NLS-1$

    /**
     * Extension used for the files in which the pattern are serialized.
     */
    public static final String PATTERN = PatternPackage.eNAME;

    /**
     * Extension used for the files in which the DQRule are serialized.
     */
    public static final String DQRULE = RulesPackage.eNAME;

    private FactoriesUtil() {
    }

    /**
     * Method "initializeAllFactories" calls static method init() for each of the factories in this project. This is
     * needed when writing EMF files.
     */
    public static void initializeAllFactories() {

        // --- talend extension packages
        ConstantsFactory.eINSTANCE.getEPackage();
        org.talend.cwm.softwaredeployment.SoftwaredeploymentFactory.eINSTANCE.getEPackage();
        RelationalFactory.eINSTANCE.getRelationalPackage();

        // --- talend DQ factories
        AnalysisFactory.eINSTANCE.getAnalysisPackage();
        DomainFactory.eINSTANCE.getDomainPackage();
        IndicatorsFactory.eINSTANCE.getIndicatorsPackage();
        ReportsFactory.eINSTANCE.getReportsPackage();
        DefinitionFactory.eINSTANCE.getDefinitionPackage();
        IndicatorSqlFactory.eINSTANCE.getIndicatorSqlPackage();

        PatternFactory.eINSTANCE.getEPackage();
        CategoryFactory.eINSTANCE.getEPackage();
        org.talend.dataquality.expressions.ExpressionsFactory.eINSTANCE.getEPackage();
        org.talend.dataquality.reports.ReportsFactory.eINSTANCE.getEPackage();
        RulesFactory.eINSTANCE.getEPackage();

        // CWM generated packages
        // TODO scorreia add other factories
        orgomg.cwm.foundation.softwaredeployment.SoftwaredeploymentFactory.eINSTANCE.getEPackage();
        orgomg.cwm.resource.relational.RelationalFactory.eINSTANCE.getEPackage();

        orgomg.cwmmip.CwmmipFactory.eINSTANCE.getEPackage();
        orgomg.mof.model.ModelFactory.eINSTANCE.getEPackage();
        orgomg.cwm.foundation.datatypes.DatatypesFactory.eINSTANCE.getEPackage();
        orgomg.cwm.objectmodel.core.CoreFactory.eINSTANCE.getEPackage();
        orgomg.cwm.objectmodel.relationships.RelationshipsFactory.eINSTANCE.getEPackage();
        orgomg.cwm.foundation.typemapping.TypemappingFactory.eINSTANCE.getEPackage();
    }

    /**
     * Method "getExtensions".
     * 
     * @return the list of file extensions
     */
    public static List<String> getExtensions() {
        List<String> extensions = new ArrayList<String>();
        // --- Talend extension packages
        extensions.add(SoftwaredeploymentPackage.eNAME);
        extensions.add(RelationalPackage.eNAME);

        // --- Talend DQ extension packages
        extensions.add(IndicatorsPackage.eNAME);
        extensions.add(DomainPackage.eNAME);
        extensions.add(CategoryPackage.eNAME);
        extensions.add(ReportsPackage.eNAME);
        extensions.add(DefinitionPackage.eNAME);
        extensions.add(IndicatorSqlPackage.eNAME);

        // --- add specific extensions
        extensions.add(PROV);
        extensions.add(ANA);
        extensions.add(CAT);
        extensions.add(REP);
        extensions.add(PATTERN);
        extensions.add(DQRULE);
        

        // --- CWM generated packages
        extensions.add(CorePackage.eNAME);
        extensions.add(TypemappingPackage.eNAME);
        // TODO scorreia add other file extensions
        return extensions;
    }

    /**
     * Method "initializeAllPackages" initializes all the EMF packages. This is needed when reading EMF files.
     */
    public static void initializeAllPackages() {

        // --- talend extension packages
        org.talend.cwm.softwaredeployment.SoftwaredeploymentPackage.eINSTANCE.getEFactoryInstance();
        // RelationalPackage.eINSTANCE.getRelationalFactory();

        // --- talend DQ factories
        // AnalysisPackage.eINSTANCE.getEFactoryInstance();
        DomainPackage.eINSTANCE.getEFactoryInstance();
        IndicatorsPackage.eINSTANCE.getEFactoryInstance();
        PatternPackage.eINSTANCE.getEFactoryInstance();
        CategoryPackage.eINSTANCE.getEFactoryInstance();
        org.talend.dataquality.expressions.ExpressionsPackage.eINSTANCE.getEFactoryInstance();
        org.talend.dataquality.reports.ReportsPackage.eINSTANCE.getEFactoryInstance();
        DefinitionPackage.eINSTANCE.getEFactoryInstance();
        IndicatorSqlPackage.eINSTANCE.getEFactoryInstance();

        // CWM generated packages
        // TODO scorreia add other packages
        orgomg.cwm.foundation.softwaredeployment.SoftwaredeploymentPackage.eINSTANCE.getEFactoryInstance();
        orgomg.cwm.resource.relational.RelationalPackage.eINSTANCE.getEFactoryInstance();

        orgomg.cwmmip.CwmmipPackage.eINSTANCE.getEFactoryInstance();
        orgomg.mof.model.ModelPackage.eINSTANCE.getEFactoryInstance();
        orgomg.cwm.foundation.datatypes.DatatypesPackage.eINSTANCE.getEFactoryInstance();
        orgomg.cwm.objectmodel.core.CorePackage.eINSTANCE.getEFactoryInstance();
        orgomg.cwm.objectmodel.relationships.RelationshipsPackage.eINSTANCE.getEFactoryInstance();
        orgomg.cwm.foundation.typemapping.TypemappingPackage.eINSTANCE.getEFactoryInstance();
    }
}
