# Reference Draft 03 — UC-02 Book Appointment

**Deliverables covered:** Fully dressed scenario (success + failure), system
sequence diagrams for success and failure, system operations and operation
contracts (iter1.pdf, Deliverables #3–#5).
**Consistency anchors:** Actors/FRs from requirement.md §3.3/§5.1; rules A2
(scheduling), A3 (referral-required services), A4 (referral validity), A6
(logging), A7.1 (appointment statuses), A8.5 (search) from draft 00; concepts
from the domain model, draft 02.
**Operation names in this file are canonical** — draft 06 reuses them unchanged.

---

## Part 1 — Fully dressed use case

| Field | Content |
|---|---|
| **Use Case ID** | UC-02 |
| **Name** | Book Appointment |
| **Scope** | Appointment Tracking System (ATS) — clinic appointment management |
| **Level** | User goal |
| **Primary Actor** | Administrative Staff; Patient may initiate the same flow for themselves (requirement.md §5.1 lists both). Contracts below are written actor-neutrally except where role matters. |
| **Stakeholders and Interests** | **Patient:** gets an appointment at a workable time, with the right professional, without travelling to a slot that cannot serve them. **Administrative Staff:** booking is fast and cannot create double-bookings they will have to untangle. **Healthcare Professional:** appointments respect their schedule and location, with referrals in place where required. **Clinic:** scheduling, referral and access rules are enforced uniformly and every booking is auditable (FR-30). |
| **Preconditions** | 1. Actor is authenticated as a `SystemUser` with role `ADMINISTRATIVE_STAFF`, or as the Patient booking for themselves. 2. The patient is registered (UC-01) — otherwise extension 7a. 3. The requested professional, specialty and clinic location exist in the system (FR-05–FR-08). |
| **Success Guarantee (Postconditions)** | 1. An `Appointment` exists linking exactly one `Patient`, one `HealthcareProfessional` and one `ClinicLocation`, with `status = Confirmed`. 2. Exactly one `TimeSlot` is occupied by that appointment; neither the professional nor the patient has an overlapping active appointment. 3. If the service required a referral (A3), the appointment is associated with a referral that was valid at booking time (A4). 4. An `ActivityLog` entry records the booking (A6). |
| **Minimal Guarantee** | No partial booking is left behind: if any check fails, no `Appointment` remains in a blocking status, no `TimeSlot` stays occupied, and the failure (with reason) is recorded in the `ActivityLog`. |
| **Trigger** | Patient or Administrative Staff ask to book an appointment for a patient. |
| **Main Success Scenario** | See below. |
| **Extensions (Failures / Alternatives)** | See below: 4a professional unavailable, 4b scheduling-constraint violation, 5a–5c referral failures, 7a patient not found, 8a booking not confirmed, 9a confirmation fails. |
| **Special Requirements** | FR-10, FR-11, FR-12 (the three verifications); NFR-01/NFR-02 (role check + logging); times to the minute (A8.4). |
| **Technology / Data Variations** | CLI entry (CON-01). Search criteria may be any FR-03 key (A8.5). |
| **Frequency** | Many times per clinic day. |
| **Open Issues** | None remaining for this use case — A2–A4 resolved the §6 questions it depends on. |

### Main Success Scenario

1. The actor asks to book an appointment and identifies the patient
   (system operation: `searchPatient(...)` if a lookup is needed first).
2. The system confirms the patient exists and displays the patient's identity
   summary (name, date of birth, coverage type — clinical data is never shown
   to Administrative Staff, FR-29).
3. The actor selects the service type, the healthcare professional (or a
   specialty, from which the system lists professionals) and the clinic
   location, and asks for availability in a date range
   (`getProfessionalAvailability(...)`).
4. The system returns only `TimeSlot`s that satisfy A2 rules 1–6 for that
   professional, location and service type.
5. The actor selects a slot and submits the booking, supplying a `referralId`
   if the service requires a referral (`requestAppointment(...)`).
6. The system verifies availability and scheduling constraints again at
   submission time, and — because the service requires a referral (A3) —
   verifies that the supplied referral is valid for this patient, specialty
   and date (A4).
7. The system creates the `Appointment` with `status = Requested`, associates
   it with the patient, professional, location, requested slot and (where
   applicable) referral. **The slot is not booked yet** — a `Requested`
   appointment holds no slot (A7.1).
8. The actor confirms the booking (`confirmAppointment(...)`).
9. The system re-verifies that the slot is still free and that the A2
   constraints and (if applicable) the referral are still valid, then books
   the slot, sets the appointment's `status = Confirmed`, records an
   `ActivityLog` entry, and displays the confirmation (appointment ID, date,
   time, professional, location).

### Extensions

**4a. Professional unavailable in the requested range**
- 4a1. `getProfessionalAvailability(...)` returns no slot (professional fully
  booked, or not scheduled at that location on those days — A2 rules 1–2).
- 4a2. The system says so, distinguishing “not working then” from “fully
  booked”. The actor picks another professional, location or date range and
  resumes at step 3. *No appointment instance is created in this extension.*

**4b. Scheduling-constraint violation on a slot that looked free**
- 4b1. The only apparently free slots violate A2 rule 3 (patient already has
  an overlapping appointment), rule 4 (slot duration does not match the
  service type) or rule 5 (slot start time is in the past).
- 4b2. The system excludes those slots and states *which* constraint removed
  them. Flow resumes at step 4 with corrected choices.

**5a. Referral required but missing**
- 5a1. At step 6 the service requires a referral (A3 — e.g. first visit to a
  Specialist Physician for that specialty) and no `referralId` was supplied,
  and no other valid referral exists for this patient + specialty.
- 5a2. The system rejects the request with reason `ReferralRequired`, creates
  **no** appointment, releases nothing (nothing was reserved), and logs the
  failure (A6).
- 5a3. The actor is told a referral must be created first (UC-08; see the
  `<<extend>>` relationship in draft 01 §5). The use case ends in failure for
  this attempt.

**5b. Referral supplied but invalid — wrong patient / wrong specialty /
wrong professional**
- 5b1. The supplied referral fails A4 rule 3 or rule 4.
- 5b2. Same handling as 5a2, with reason `ReferralNotValid` and a message
  identifying which match failed. Ends in failure.

**5c. Referral expired, rejected, cancelled or still a draft**
- 5c1. The supplied referral fails A4 rule 1 or rule 2 (status `Draft`,
  `Rejected`, `Expired`, `Fulfilled` or `Cancelled`, or past `expiryDate`).
- 5c2. Same handling as 5a2, reason `ReferralNotValid`, message naming the
  referral's actual status. Ends in failure. (This is the failure case shown
  in the failure SSD below.)

**7a. Patient not found**
- 7a1. `searchPatient(...)` returns no match (or several matches and the
  actor selects none, A8.5).
- 7a2. If the patient was never registered, the actor is directed to UC-01
  Register Patient. This use case ends; no booking is attempted, and the
  search itself is logged if it used patient-identifying criteria (A6).

**8a. Booking is not confirmed (actor cancels the request)**
- 8a1. Instead of confirming, the actor cancels the request (UC-04 Cancel
  Appointment).
- 8a2. The appointment's `status` becomes `Cancelled` and the cancellation
  is logged. **No slot is released, because none was ever booked** — the
  slot was never taken away from other patients.
- 8a3. If the actor simply leaves without confirming or cancelling, the
  appointment stays `Requested`. Because a `Requested` appointment holds no
  slot (A7.1), this blocks nobody; it can be confirmed later (step 8) or
  cancelled (8a1).

**9a. Confirmation fails (slot or referral no longer valid)**
- 9a1. At step 9 the re-verification fails: another appointment was
  confirmed for the same slot in the meantime, an A2 constraint no longer
  holds (e.g. the patient now has an overlapping confirmed appointment), or
  the referral is no longer valid under A4 (e.g. it expired).
- 9a2. The system rejects the confirmation with reason
  `ProfessionalUnavailable`, `SchedulingConstraintViolation` or
  `ReferralNotValid`. The appointment's `status` becomes `Cancelled`; **no
  slot is booked and no slot changes state**; the failure and its reason are
  logged (A6).
- 9a3. The actor may start a new booking from step 3 with another slot.

---

## Part 2 — System Sequence Diagrams (system as black box)

In an SSD the system is one lifeline; only actor↔system messages are system
operations. The A2/A4 *verifications are internal system responsibilities*
triggered inside `requestAppointment(...)`, not separate actor messages —
that is why they appear as SSD notes, not arrows.

### 2.1 SSD — Success scenario

```plantuml
@startuml
actor "Administrative Staff\n(or Patient)" as Actor
participant ": ATS (System)" as System

Actor -> System : searchPatient(searchCriteria)
System --> Actor : patientSummary(patientId, name, dateOfBirth, coverageType)

Actor -> System : getProfessionalAvailability(professionalId, locationId, serviceType, dateRange)
System --> Actor : availableSlots [only slots satisfying A2 rules 1-6]

Actor -> System : requestAppointment(patientId, professionalId, locationId, serviceType, slotId, referralId)
note right of System
  Internally (not actor messages):
  re-verify availability + A2 constraints,
  and referral validity per A3/A4 if required.
  Creates Appointment with status = Requested
  and records the requested slot.
  The slot is NOT booked yet.
end note
System --> Actor : appointmentCreated(appointmentId, status = Requested)

Actor -> System : confirmAppointment(appointmentId)
note right of System
  Re-verifies slot still free, A2 constraints
  and referral (if any). Books the TimeSlot
  only now; status = Confirmed.
end note
System --> Actor : bookingConfirmed(appointmentId, status = Confirmed, dateTime, professional, location)
@enduml
```
<img width="843" height="520" alt="Screenshot 2026-10-23 at 10 40 04 PM" src="https://github.com/user-attachments/assets/55797460-bd37-4432-87f0-b13a8978d6c1" />


### 2.2 SSD — Failure scenario (referral required but invalid/expired; covers extensions 5a–5c)

```plantuml
@startuml
actor "Administrative Staff\n(or Patient)" as Actor
participant ": ATS (System)" as System

Actor -> System : searchPatient(searchCriteria)
System --> Actor : patientSummary(patientId, name, dateOfBirth, coverageType)

Actor -> System : getProfessionalAvailability(professionalId, locationId, serviceType, dateRange)
System --> Actor : availableSlots

Actor -> System : requestAppointment(patientId, professionalId, locationId, serviceType, slotId, referralId)
note right of System
  Referral check (A3/A4) fails:
  referral is missing, or its status is
  Draft / Rejected / Expired / Fulfilled / Cancelled,
  or it belongs to another patient / specialty.
  No Appointment instance is created,
  no TimeSlot is occupied,
  failure + reason are written to ActivityLog (A6).
end note
System --> Actor : bookingRejected(reason = ReferralRequired / ReferralNotValid)

note over Actor, System
  Failure equivalent for extensions 4a/4b:
  getProfessionalAvailability returns an empty list, or
  requestAppointment returns bookingRejected(reason =
  ProfessionalUnavailable / SchedulingConstraintViolation).
  End state is identical: nothing created, failure logged.

  Failure at confirmation (extension 9a):
  confirmAppointment returns confirmationRejected(reason).
  Appointment status = Cancelled; no slot was ever
  booked, so no slot changes state; failure logged.
end note
@enduml
```
<img width="869" height="569" alt="Screenshot 2026-10-23 at 10 41 04 PM" src="https://github.com/user-attachments/assets/04fe4ec7-91cd-414d-b440-ec1b441d5717" />

### 2.3 System operations visible in these SSDs

| # | Operation | Appears in |
|---|---|---|
| OP-02-1 | `searchPatient(searchCriteria)` | Success + failure (shared with UC-06, UC-08 — same name/signature in drafts 04–06) |
| OP-02-2 | `getProfessionalAvailability(professionalId, locationId, serviceType, dateRange)` | Success + failure |
| OP-02-3 | `requestAppointment(patientId, professionalId, locationId, serviceType, slotId, referralId [0..1])` | Success + failure |
| OP-02-4 | `confirmAppointment(appointmentId)` | Success (books the slot); failure noted in the failure SSD (extension 9a) |

---

## Part 3 — Operation contracts (Larman style)

### Contract CO-02-1: `searchPatient`

| Field | Content |
|---|---|
| **Operation** | `searchPatient(searchCriteria)` — where `searchCriteria` is one of: health insurance number, date of birth, phone number, partial first name and/or partial last name (FR-03) |
| **Cross-References** | UC-02 (also UC-09, UC-06, UC-08); FR-03, FR-28, FR-30 |
| **Preconditions** | Actor is authenticated; actor's role permits patient lookup (`ADMINISTRATIVE_STAFF`, `HEALTHCARE_PROFESSIONAL` / `AUTHORIZED_CLINICAL_USER`, or the Patient searching only for themselves). |
| **Postconditions** | – No domain instance was created, deleted or modified (read-only operation). – The matching `Patient` summaries (identity fields only, no clinical data) were returned; if several patients match (A8.5), all matches are returned for actor selection. – An `ActivityLog` entry was created recording actor, criteria type, number of matches and `outcome = Success`, with `patientId` set when exactly one patient matched (A6). |

### Contract CO-02-2: `getProfessionalAvailability`

| Field | Content |
|---|---|
| **Operation** | `getProfessionalAvailability(professionalId, locationId, serviceType, dateRange)` |
| **Cross-References** | UC-02, UC-03, UC-10; FR-07, FR-08, FR-10, FR-11 |
| **Preconditions** | A `HealthcareProfessional` with `professionalId` exists; a `ClinicLocation` with `locationId` exists; the professional practises at that location (otherwise the result is legitimately empty — extension 4a, not an error of the operation). |
| **Postconditions** | – No domain instance was created, deleted or modified. – The returned set contains exactly the `TimeSlot`s of that professional's `Schedule` at that location, inside `dateRange`, whose duration matches `serviceType` (A2 rule 4) and which are not booked by an appointment in status `Confirmed`, `CheckedIn` or `InConsultation` (A2 rules 1, 2, 5). `Requested` appointments do not make a slot unavailable, because they hold no slot (A7.1). |

### Contract CO-02-3: `requestAppointment`

| Field | Content |
|---|---|
| **Operation** | `requestAppointment(patientId, professionalId, locationId, serviceType, slotId, referralId [0..1])` |
| **Cross-References** | UC-02; FR-09, FR-10, FR-11, FR-12, FR-13 |
| **Preconditions** | 1. A `Patient` with `patientId`, a `HealthcareProfessional` with `professionalId`, and a `TimeSlot` with `slotId` all exist. 2. The slot belongs to that professional's schedule at `locationId`. 3. At submission time the slot still satisfies A2 rules 1–6 (re-verified inside the operation — availability shown earlier is not a guarantee). 4. If `serviceType` requires a referral (A3), a `referralId` is supplied **or** another referral exists that is valid for this patient/specialty/date under A4. |
| **Postconditions (success)** | – An `Appointment` instance `a` was created. – `a` was associated with exactly one `Patient`, one `HealthcareProfessional`, one `ClinicLocation`, and with the chosen `TimeSlot` as its **requested** slot. – `a.status` became `Requested` (A7.1). – `a.serviceType` became the requested service type. – The `TimeSlot` was **not** booked: `TimeSlot.isBooked` is unchanged (`false`) and no “occupies” association was created. The slot is booked only by `confirmAppointment(...)`. – If a referral was required, `a` was associated with the valid `Referral` used; the referral's own status was **not** changed (A4 rule 5 — referrals are not consumed by booking). – An `ActivityLog` entry was created (`outcome = Success`, A6). |
| **Postconditions (failure)** | – No `Appointment` was created and no `TimeSlot` changed state (minimal guarantee). – An `ActivityLog` entry was created with `outcome = Failure` and `failureReason` one of `ProfessionalUnavailable`, `SchedulingConstraintViolation`, `ReferralRequired`, `ReferralNotValid`. |

### Contract CO-02-4: `confirmAppointment`

| Field | Content |
|---|---|
| **Operation** | `confirmAppointment(appointmentId)` |
| **Cross-References** | UC-02; FR-13 |
| **Preconditions** | An `Appointment` with `appointmentId` exists with `status = Requested`. (Whether its requested slot is still free, the A2 constraints still hold and its referral is still valid is **re-checked by the operation**, not assumed — see the failure postconditions.) |
| **Postconditions (success)** | – The appointment became associated with its requested `TimeSlot` through “occupies”, and `TimeSlot.isBooked` became `true`. This is the only operation in UC-02 that books a slot. – The appointment's `status` became `Confirmed` (A7.1). – An `ActivityLog` entry recording the confirmation was created (A6). |
| **Postconditions (failure — extension 9a)** | – The requested slot was already booked by another appointment, an A2 constraint no longer held, or the referral was no longer valid (A4). – The appointment's `status` became `Cancelled`. – No `TimeSlot` changed state: no slot was booked, and there was nothing to release because a `Requested` appointment never held one. – An `ActivityLog` entry was created with `outcome = Failure` and `failureReason` one of `ProfessionalUnavailable`, `SchedulingConstraintViolation`, `ReferralNotValid`. |
| **Notes — what happens to slots on cancellation** | – **Cancelled before confirmation** (extension 8a, via UC-04): `status` becomes `Cancelled`; no slot is released because none was booked. – **Cancelled after confirmation** (UC-04, specified in a later iteration): `status` becomes `Cancelled`, the “occupies” association is removed and `TimeSlot.isBooked` becomes `false`, so the slot is available again. – A `Requested` appointment that is never confirmed blocks no slot, so no timeout or release rule is needed. |
