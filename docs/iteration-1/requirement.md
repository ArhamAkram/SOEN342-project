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

*Note: These are initial actor candidates. The final use-case diagram will distinguish
