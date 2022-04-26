/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.mchms;

import org.openmrs.module.kenyaemr.reporting.cohort.definition.pmtct.MCHMissedAppointmentsCohortDefinition;
import org.openmrs.PatientIdentifierType;
import org.openmrs.module.kenyacore.report.HybridReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractHybridReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyacore.report.data.patient.definition.CalculationDataDefinition;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.*;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.EncounterType;
import org.openmrs.module.reporting.common.TimeQualifier;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.springframework.stereotype.Component;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.maternity.MaternityPatientStatusDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.CalculationResultConverter;
import org.openmrs.module.kenyaemr.calculation.library.NumberOfDaysLateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.LastReturnVisitDateCalculation;
import org.openmrs.module.kenyaemr.reporting.calculation.converter.EncounterDatetimeConverter;
import org.openmrs.module.reporting.data.patient.definition.EncountersForPatientDataDefinition;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.reporting.common.SortCriteria;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({"kenyaemr.mchms.report.patientsMissedAppointmentsMCH"})
public class MCHMissedAppointmentsReportBuilder extends AbstractHybridReportBuilder {
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    /**
     *
     * @see org.openmrs.module.kenyacore.report.builder.AbstractCohortReportBuilder#addColumns(org.openmrs.module.kenyacore.report.CohortReportDescriptor, PatientDataSetDefinition)
     */
    @Override
    protected void addColumns(HybridReportDescriptor report, PatientDataSetDefinition dsd) {

        PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
        DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(upn.getName(), upn), identifierFormatter);
        PersonAttributeType phoneNumber = MetadataUtils.existing(PersonAttributeType.class, CommonMetadata._PersonAttributeType.TELEPHONE_CONTACT);
        DataConverter formatter = new ObjectFormatter("{familyName}, {givenName}");
        DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), formatter);

        dsd.addSortCriteria("Number of days late", SortCriteria.SortDirection.ASC);
        dsd.setName("Patients Missed Appointments on MCH");
        dsd.addColumn("id", new PersonIdDataDefinition(), "");
        dsd.addColumn("Name", nameDef, "");
        dsd.addColumn("Unique Patient number", identifierDef, "");
        dsd.addColumn("Age", new AgeDataDefinition(), "", null);
        dsd.addColumn("Sex", new GenderDataDefinition(), "", null);
        dsd.addColumn("Phone Number", new PersonAttributeDataDefinition(phoneNumber), "");

        EncountersForPatientDataDefinition definition = new EncountersForPatientDataDefinition();
        EncounterType hivConsultation = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
        EncounterType hivEnrollment = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_ENROLLMENT);
        EncounterType consultation = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);

        List<EncounterType> encounterTypes = Arrays.asList(hivConsultation, consultation, hivEnrollment);

        definition.setWhich(TimeQualifier.LAST);
        definition.setTypes(encounterTypes);
        dsd.addColumn("Last Visit Date", definition, "", new EncounterDatetimeConverter());
        dsd.addColumn("Last HIV Appointment date", new CalculationDataDefinition("Appointment date", new LastReturnVisitDateCalculation()), "", new DataConverter[]{new CalculationResultConverter()});
        dsd.addColumn("Number of days late", new CalculationDataDefinition("Number of days late", new NumberOfDaysLateCalculation()), "", new DataConverter[]{new CalculationResultConverter()});
        dsd.addColumn("Patient Status", new MaternityPatientStatusDataDefinition(), "", null);

    }

    @Override
    protected Mapped<CohortDefinition> buildCohort(HybridReportDescriptor descriptor, PatientDataSetDefinition dsd) {
        CohortDefinition cd = new MCHMissedAppointmentsCohortDefinition();
        cd.setName("Patients Missed Appointments on MCH");
        return ReportUtils.map(cd, "");
    }
}
