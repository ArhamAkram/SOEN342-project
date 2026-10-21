# Reference Draft 06 — System Operations and Operation Contracts (Consolidated)

**Deliverable covered:** Identification of system operations and operation
contracts (iter1.pdf, Deliverable #5), consolidated across the three detailed
use cases.
**Rule for this file:** it introduces **no new operation names**. Every name,
signature and contract below is copied from drafts 03–05; if this file and a
use-case file ever disagreed, the use-case file is where the fix belongs and
this file must be regenerated from it. That rule is itself a consistency
device you can state in your own submission.

---

## 1. Consolidated table of all system operations

Operations are listed in the order an actor meets them across UC-02 → UC-06 →
UC-08. `searchPatient(...)` appears in all three SSD sets and is counted once.

| # | System operation (canonical signature) | Source UC | Source SSD(s) | Contract |
|---|---|---|---|---|
| 1 | `searchPatient(searchCriteria)` | UC-02, UC-06 (pre-step), UC-08 | UC-02 success + failure; UC-08 success + failure | CO-02-1 (draft 03) |
| 2 | `getProfessionalAvailability(professionalId, locationId, serviceType, dateRange)` | UC-02 | UC-02 success + failure | CO-02-2 (draft 03) |
| 3 | `requestAppointment(patientId, professionalId, locationId, serviceType, slotId, referralId [0..1])` | UC-02 | UC-02 success + failure | CO-02-3 (draft 03) |
| 4 | `confirmAppointment(appointmentId)` | UC-02 | UC-02 success | CO-02-4 (draft 03) |
| 5 | `getAppointment(appointmentId)` | UC-06 | UC-06 success + failure | CO-06-1 (draft 04) |
| 6 | `startConsultation(appointmentId, professionalId)` | UC-06 | UC-06 success + failure | CO-06-2 (draft 04) |
| 7 | `recordClinicalInformation(consultationId, observations, diagnoses, treatmentRecommendations, followUpRequirements [0..1])` | UC-06 (incl. UC-07) | UC-06 success | CO-06-3 (draft 04) |
| 8 | `issuePrescription(consultationId, medicationList)` | UC-06 | UC-06 success (optional `opt` block) | CO-06-4 (draft 04) |
| 9 | `requestFollowUpAppointment(consultationId, followUpTimeframe, reason)` | UC-06 | UC-06 success (optional `opt` block) | CO-06-5 (draft 04) |
| 10 | `completeConsultation(consultationId)` | UC-06 | UC-06 success | CO-06-6 (draft 04) |
| 11 | `getAvailableSpecialties()` | UC-08 | UC-08 success + failure | CO-08-2 (draft 05) |
| 12 | `createReferral(patientId, referringProfessionalId, requestedSpecialtyId [0..1], requestedProfessionalId [0..1], clinicalReason, priority)` | UC-08 | UC-08 success + failure | CO-08-3 (draft 05) |
| 13 | `submitReferral(referralId)` | UC-08 | UC-08 success | CO-08-4 (draft 05) |

**Total: 13 distinct system operations** (4 introduced by UC-02, 6 by UC-06,
3 new by UC-08 plus the shared `searchPatient`).

### 1.1 Operations deliberately *not* in the list

- **Verify availability / verify scheduling constraints / verify referral
  requirement** (draft 01 includes) are internal responsibilities executed
  inside `requestAppointment(...)`, not actor→system messages, so they are
  not system operations in the SSD sense. Listing them as operations would
  double-count step 6 of UC-02's main success scenario.
- **Register arrival (UC-05), reschedule (UC-03), cancel (UC-04), generate
  invoice (UC-12), register payment (UC-13)** are real use cases, but
  iter1.pdf requires SSDs/contracts only for the three detailed critical use
  cases. Their operations (`registerArrival`, `generateInvoice`, …) belong to
  a later iteration's list; naming them here without diagrams or scenarios
  would create the inconsistency this file exists to prevent. UC-05 is
  *referenced* (its outcome, status `CheckedIn`, is UC-06's precondition)
  without being specified — a deliberate boundary, stated openly.

---

## 2. Summary contract table

One row per operation: preconditions and postconditions **summarised from**
the full contracts in drafts 03–05 (contract ID given for the full text).
Statuses use the canonical vocabularies of draft 00, A7. “Logged” = an
`ActivityLog` entry per A6, including failures and denials.

| Operation | Contract | Preconditions (summary) | Postconditions (summary) |
|---|---|---|---|
| `searchPatient(searchCriteria)` | CO-02-1 | Actor authenticated; role may look up patients (or patient searching for self only). | Read-only; matching patient summaries returned (all matches if several, A8.5); logged with `patientId` when exactly one match. |
| `getProfessionalAvailability(professionalId, locationId, serviceType, dateRange)` | CO-02-2 | Professional and location exist. | Read-only; returns exactly the schedule `TimeSlot`s at that location in range, duration-matched to service type, not occupied by an active appointment (A2 rules 1, 2, 4, 5). Empty result is legitimate (UC-02 ext. 4a). |
| `requestAppointment(patientId, professionalId, locationId, serviceType, slotId, referralId [0..1])` | CO-02-3 | Patient, professional, slot exist; slot is that professional's at that location; A2 rules 1–6 re-verified at submission; if service needs a referral (A3), one valid under A4 exists/was supplied. | Success: `Appointment` created, linked to patient + professional + location + slot, `status = Requested`, slot booked, linked to the valid referral if one was required (referral status unchanged), logged. Failure: nothing created, nothing reserved, failure logged with reason (`ProfessionalUnavailable` / `SchedulingConstraintViolation` / `ReferralRequired` / `ReferralNotValid`). |
| `confirmAppointment(appointmentId)` | CO-02-4 | Appointment exists with `status = Requested` and its slot still held/valid. | `status` becomes `Confirmed`; slot remains booked; logged. Unconfirmed appointments release their slot at session end (UC-02 ext. 8a). |
| `getAppointment(appointmentId)` | CO-06-1 | Appointment exists; actor authenticated. | Read-only; summary returned filtered to the actor's role (staff: scheduling fields only, FR-29); access logged with `patientId`. |
| `startConsultation(appointmentId, professionalId)` | CO-06-2 | Appointment `status = CheckedIn`; actor is the assigned/authorised professional (never an administrative role); no consultation already in progress for it. | Success: `Consultation` created (linked to appointment + professional, start time set), appointment `status = InConsultation`, logged. Failure: nothing created, status unchanged, `Failure` (`PatientNotCheckedIn`, …) or `AccessDenied` logged. |
| `recordClinicalInformation(consultationId, observations, diagnoses, treatmentRecommendations, followUpRequirements [0..1])` | CO-06-3 | Consultation in progress; actor is conducting/authorised professional. | The consultation's single `ClinicalRecord` created (or updated on repeat call — never a second one), fields + timestamp set, linked to the consultation; logged (fact of recording, not content, A6). |
| `issuePrescription(consultationId, medicationList)` | CO-06-4 | Consultation in progress; authorised professional; `medicationList` has ≥ 1 medication. | `Prescription` created for the consultation with issue time set, associated with its 1..* `Medication`s; logged. Empty list ⇒ rejected, nothing created (UC-06 ext. 8a). |
| `requestFollowUpAppointment(consultationId, followUpTimeframe, reason)` | CO-06-5 | Consultation in progress or just completed by the same professional; follow-up requirement exists/supplied. | Requirement recorded on `ClinicalRecord.followUpRequirements`; pending follow-up booking request created for same patient + professional/specialty; **no slot reserved, no appointment confirmed** (that happens only via UC-02 with A2–A4); logged. |
| `completeConsultation(consultationId)` | CO-06-6 | Consultation in progress; `ClinicalRecord` exists with observations + diagnoses; `feeAmount` recorded if service uninsured. | End time set; appointment `status = Completed`; uninsured consultation invoice-ready for UC-12 by Administrative Staff (no `Invoice` created here, A5); logged. |
| `getAvailableSpecialties()` | CO-08-2 | Actor may create referrals or enter external ones. | Read-only; all `Specialty` records returned, each flagged whether any non-external professional holding it practises at the clinic (makes UC-08 ext. 4a avoidable). |
| `createReferral(patientId, referringProfessionalId, requestedSpecialtyId [0..1], requestedProfessionalId [0..1], clinicalReason, priority)` | CO-08-3 | Patient and referrer exist (referrer may be external, `isExternal = true`, A8.3); actor is the referrer or entering on their behalf; `clinicalReason` non-empty; ≥ 1 of specialty/professional supplied and valid (and consistent if both); `priority ∈ {Routine, Urgent, Emergent}`. | Success: `Referral` created linked to patient + referrer + requested specialty/professional, reason + priority stored, `status = Draft`, `issuedDate` = today, `expiryDate` per A4 rule 2; logged (incl. “entered on behalf of” for external). Failure: nothing created; `Failure` (`MissingClinicalReason` / `InvalidSpecialty` / `InvalidRequestedProfessional`) or `AccessDenied` logged. |
| `submitReferral(referralId)` | CO-08-4 | Referral exists with `status = Draft` and still satisfies CO-08-3's field conditions; actor is its referrer / entered it on their behalf. | `status` becomes `Submitted` — from now on valid for booking under A4, usable by `requestAppointment`, and able to lead to 1..* appointments (FR-24) without booking alone changing its status; logged. |

---

## 3. Consistency checks performed across drafts 03–05 (replicate these in your own work)

1. **Status words:** every postcondition above uses only statuses defined in
   draft 00 A7.1 (`Appointment`) and A7.2 (`Referral`). No file invents a
   status (in particular, there is no `Rescheduled` status — draft 00 explains
   why).
2. **Operation names:** §1's signatures character-match the SSD message labels
   in drafts 03–05, including the `[0..1]` optional parameters
   (`referralId`, `followUpRequirements`, `requestedSpecialtyId`,
   `requestedProfessionalId`).
3. **Shared operation, single contract:** `searchPatient(...)` has exactly one
   contract (CO-02-1) referenced by UC-06 and UC-08, instead of three
   near-identical contracts that could drift.
4. **Cross-use-case hand-offs close the loop:** UC-08's output (`Referral`,
   `Submitted`) is exactly UC-02's input condition (A4); UC-05's output
   (status `CheckedIn`, not specified here) is exactly UC-06's precondition;
   UC-06's output (`Completed` + `feeAmount`) is exactly UC-12's input
   (A5). No hand-off depends on data another file never creates.
5. **Failure symmetry:** every operation that can fail states both success
   *and* failure postconditions, and every failure postcondition ends with a
   logged outcome (A6) — there is no silent failure anywhere in the set.
