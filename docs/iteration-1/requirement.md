# Appointment Tracking System — Requirements Specification

**Course:** SOEN 342 — Software Requirements and Deployment  
**Project:** Iteration 1 — System Modelling and Requirements Specification  
**Semester:** Fall 2026

## 1. Introduction

### 1.1 Purpose

The purpose of this document is to identify and organize the initial requirements of the Appointment Tracking System (ATS), based on the system description provided for Iteration 1.

The requirements will serve as a foundation for the use-case diagram, UML domain model, system sequence diagrams, fully dressed use cases, and system operation contracts.

### 1.2 System Overview

The Appointment Tracking System is designed for medical clinics to manage patients, healthcare professionals, appointments, consultations, referrals, prescriptions, clinical records, invoices, and payments.

The system supports administrative staff and healthcare professionals in performing their respective tasks while maintaining appropriate access restrictions to protect confidential patient information.

### 1.3 Scope

The final system will be implemented as a command-line application (CLI) using an object-oriented programming platform.

The following integrations are outside the current system scope:

- Electronic claims submission to RAMQ.
- Electronic prescription transmission to pharmacies.
- Integration with provincial healthcare systems.

## 2. System Actors

| Actor | Description |
|---|---|
| Patient | Individual receiving healthcare services and requesting appointments. |
| Administrative Staff | Personnel responsible for patient registration, appointment management, and recording patient arrivals. |
| Healthcare Professional | Family physician, specialist, nurse, or other authorized professional providing medical services. |
| Referring Professional | Healthcare professional issuing consultation requests, including external physicians. |
| Authorized Clinical User | Healthcare professional with permission to access or modify clinical information. |

*Note: These are initial actor candidates. The final use-case diagram will distinguish actors based on their interactions with the system.*

## 3. Functional Requirements

### 3.1 Patient Management

**FR-01:** The system shall uniquely identify each registered patient.

**FR-02:** The system shall maintain patient information, including relevant identification and contact information.

**FR-03:** The system shall support searching for patients using:

- Health insurance number.
- Date of birth.
- Phone number.
- Partial first name.
- Partial last name.

**FR-04:** The system shall support patients with RAMQ coverage, private insurance, or uninsured services.

### 3.2 Healthcare Professional Management

**FR-05:** The system shall maintain information about healthcare professionals.

**FR-06:** The system shall maintain healthcare professionals' specialties.

**FR-07:** The system shall maintain healthcare professionals' schedules.

**FR-08:** The system shall support healthcare professionals practising at multiple clinic locations.

### 3.3 Appointment Management

**FR-09:** The system shall allow patients or authorized clinic staff to request appointments.

**FR-10:** The system shall verify healthcare professional availability before confirming an appointment.

**FR-11:** The system shall verify applicable scheduling constraints.

**FR-12:** The system shall verify referral requirements when an appointment requires a consultation request.

**FR-13:** The system shall support appointment confirmation.

**FR-14:** The system shall support appointment rescheduling.

**FR-15:** The system shall support appointment cancellation.

**FR-16:** The system shall allow administrative staff to register a patient's arrival on the day of an appointment.

### 3.4 Consultation Management

**FR-17:** The system shall support recording consultations performed by healthcare professionals.

**FR-18:** The system shall allow authorized healthcare professionals to record relevant clinical information, including:

- Observations.
- Diagnoses.
- Treatment recommendations.
- Follow-up requirements.

**FR-19:** The system shall support issuing prescriptions containing one or more medications.

**FR-20:** The system shall support requests for follow-up appointments.

### 3.5 Referral Management

**FR-21:** The system shall support consultation requests between healthcare professionals.

**FR-22:** The system shall maintain the following referral information:

- Referring healthcare professional.
- Requested specialty or healthcare professional.
- Clinical reason.
- Priority.
- Referral status.

**FR-23:** The system shall support referrals originating from external physicians.

**FR-24:** The system shall allow a referral to be associated with one or more appointments.

### 3.6 Billing and Payment Management

**FR-25:** The system shall record applicable fees for uninsured medical services.

**FR-26:** The system shall generate invoices for uninsured services.

**FR-27:** The system shall register payments associated with uninsured services.

### 3.7 Access Control and Accountability

**FR-28:** The system shall restrict access to clinical and administrative information according to user roles.

**FR-29:** The system shall allow administrative staff to manage patient registration and appointments without granting them the same clinical information access as authorized healthcare professionals.

**FR-30:** The system shall record relevant system activities to support accountability and patient confidentiality.

## 4. Non-Functional Requirements and Constraints

The following requirements and constraints are derived from the assignment description. Additional measurable quality requirements may be defined in later iterations.

**NFR-01 — Confidentiality:** The system shall protect confidential patient information through role-based access restrictions.

**NFR-02 — Accountability:** The system shall record relevant activities to support accountability.

**CON-01 — Interface:** The final application shall operate through a command-line interface without a graphical or web interface.

**CON-02 — Implementation:** The core system shall be implemented using an object-oriented platform.

**CON-03 — External Integrations:** Electronic RAMQ claims submission, electronic prescribing to pharmacies, and provincial system integrations are outside the current scope.

## 5. Initial Use-Case Identification

The following classifications are proposed by the project team for Iteration 1 and are not explicitly assigned in the project description.

### 5.1 Critical Use Cases

| ID | Use Case | Primary Actor |
|---|---|---|
| UC-01 | Register Patient | Administrative Staff |
| UC-02 | Book Appointment | Administrative Staff / Patient |
| UC-03 | Reschedule Appointment | Administrative Staff / Patient |
| UC-04 | Cancel Appointment | Administrative Staff / Patient |
| UC-05 | Register Patient Arrival | Administrative Staff |
| UC-06 | Conduct Consultation | Healthcare Professional |
| UC-07 | Record Clinical Information | Healthcare Professional |
| UC-08 | Create Medical Referral | Healthcare Professional |

### 5.2 Non-Critical Use Cases

| ID | Use Case | Primary Actor |
|---|---|---|
| UC-09 | Search Patient | Administrative Staff |
| UC-10 | View Professional Schedule | Authorized Clinic Staff |
| UC-11 | View Appointment Details | Authorized User |
| UC-12 | Generate Invoice | Authorized Clinic Staff |
| UC-13 | Register Payment | Authorized Clinic Staff |
| UC-14 | View Clinic Information | Authorized User |

### 5.3 Selected Critical Use Cases for Detailed Analysis

The following three use cases are proposed for fully dressed scenarios, success and failure system sequence diagrams, and corresponding operation contracts:

1. **UC-02 — Book Appointment**
2. **UC-06 — Conduct Consultation**
3. **UC-08 — Create Medical Referral**

These use cases represent important workflows involving scheduling, clinical information, and coordination between healthcare professionals.

## 6. Assumptions and Open Questions

The initial description does not specify every business rule. The following items require clarification or explicit assumptions by the project team:

- Which patient information fields are mandatory during registration?
- What scheduling constraints apply beyond professional availability?
- Which medical services require referrals?
- What conditions determine whether a referral is valid?
- Which user roles may generate invoices and register payments?
- What information must be recorded in the activity log?
- What appointment and referral status values are supported?

These questions should be resolved consistently across the UML diagrams, scenarios, and operation contracts.

## 7. Requirements Traceability

The functional requirements will be mapped to the corresponding use cases and system operations.

| Requirement IDs | Related Use Cases |
|---|---|
| FR-01 to FR-04 | UC-01, UC-09 |
| FR-05 to FR-08 | UC-10 |
| FR-09 to FR-15 | UC-02, UC-03, UC-04 |
| FR-16 | UC-05 |
| FR-17 to FR-20 | UC-06, UC-07 |
| FR-21 to FR-24 | UC-08 |
| FR-25 to FR-27 | UC-12, UC-13 |
| FR-28 to FR-30 | Cross-cutting access control and accountability requirements |

This mapping is preliminary and will be refined as the system models are developed.

## 8. References

SOEN 342 — Software Requirements and Deployment, *Project — Iteration I: System Modelling and Requirements Specification*, Fall 2026, pages 1–4.
