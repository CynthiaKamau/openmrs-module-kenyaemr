/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.cohort.definition.evaluator.pmtct;

import org.openmrs.module.kenyaemr.reporting.cohort.definition.pmtct.MCHMissedAppointmentsCohortDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.evaluator.CohortDefinitionEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Evaluator for MCHMissedAppointmentsCohortDefinition
 * Includes patients who are on MCH.
 */
@Handler(supports = {MCHMissedAppointmentsCohortDefinition.class})
public class MCHMissedAppointmentsCohortDefinitionEvaluator implements CohortDefinitionEvaluator {

    private final Log log = LogFactory.getLog(this.getClass());

    @Autowired
    EvaluationService evaluationService;

    @Override
    public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {
        MCHMissedAppointmentsCohortDefinition definition = (MCHMissedAppointmentsCohortDefinition) cohortDefinition;

        if (definition == null)
            return null;

        Cohort newCohort = new Cohort();
        String qry="select t.patient_id from(select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date," +
                "greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date," +
                "greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date, " +
                "greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca," +
                "d.patient_id as disc_patient, d.effective_disc_date as effective_disc_date, max(d.visit_date) as date_discontinued, " +
                "d.discontinuation_reason, de.patient_id as started_on_drugs from kenyaemr_etl.etl_patient_hiv_followup fup join kenyaemr_etl.etl_patient_demographics p " +
                "on p.patient_id=fup.patient_id join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id join kenyaemr_etl.etl_patient_program l " +
                "on fup.patient_id = l.patient_id left outer join kenyaemr_etl.etl_drug_event de on e.patient_id = de.patient_id and de.program='HIV' and " +
                "date(date_started) <= date(curdate()) left outer JOIN (select patient_id, coalesce(date(effective_discontinuation_date),visit_date)" +
                " visit_date,max(date(effective_discontinuation_date)) as effective_disc_date,discontinuation_reason from kenyaemr_etl.etl_patient_program_discontinuation" +
                " where date(visit_date) <= date(curdate()) and program_name='HIV' group by patient_id) d on d.patient_id = fup.patient_id where" +
                " fup.visit_date <= date(curdate()) and l.program ='MCH-Mother Services' group by patient_id having ((timestampdiff(DAY,date(latest_tca),date(curdate())) " +
                "between 1 and 30) and ((date(d.effective_disc_date) > date(curdate()) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null) " +
                "and (date(latest_vis_date) > date(date_discontinued) and date(latest_tca) > date(date_discontinued) or disc_patient is null))) t;";

        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.append(qry);
        List<Integer> ptIds = evaluationService.evaluateToList(builder, Integer.class, context);
        newCohort.setMemberIds(new HashSet<Integer>(ptIds));
        return new EvaluatedCohort(newCohort, definition, context);
    }
}
