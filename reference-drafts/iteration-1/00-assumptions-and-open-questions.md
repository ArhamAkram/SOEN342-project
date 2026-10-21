# Reference Draft 00 — Assumptions and Open Questions Resolved

**System:** Appointment Tracking System (ATS) — SOEN 342, Fall 2026, Iteration 1
**Purpose:** Resolve, explicitly and consistently, every open question listed in
`docs/iteration-1/requirement.md` §6, plus the status vocabularies that the
scenarios, diagrams and contracts depend on.
**Status of this document:** Reference draft for comparison / learning. Every
other reference draft (01–06) uses the decisions made here, word for word.

> How to read this file: each assumption has an ID (A1, A2, …), the question it
> answers, the decision, and the rationale. If you change a decision in your own
> work, change it everywhere — the graders check consistency across artifacts,
> not whether you picked the same answer as this draft.

---

## A1. Mandatory patient information fields (answers §6, bullet 1)

**Question:** Which patient information fields are mandatory during registration?

**Decision.** A patient record cannot be created unless all of the following
are present (FR-01, FR-02):

| Field | Mandatory? | Notes |
|---|---|---|
| `firstName` | Yes | Non-empty text. |
| `lastName` | Yes | Non-empty text. |
| `dateOfBirth` | Yes | Must be a past date; used as a search key (FR-03). |
| `phoneNumber` | Yes | At least one reachable contact number; search key (FR-03). |
| `address` | Yes | Civic address for clinic correspondence. |
| `coverageType` | Yes | One of `RAMQ`, `Private`, `Uninsured` (see A5 / FR-04). |
| `healthInsuranceNumber` | Conditional | Mandatory **if and only if** `coverageType` is `RAMQ` or `Private`. The system description says *most* (not all) patients have a health insurance number, so an uninsured patient must be registrable without one. |
| `emailAddress` | No | Optional secondary contact. |
| `emergencyContact` | No | Optional in Iteration 1. |

**Uniqueness rule (supports FR-01):** `patientId` is a system-generated,
immutable unique identifier. `healthInsuranceNumber`, when present, must also
be unique across patients — a duplicate is a registration error, not a second
record for the same person.

**Rationale:** The mandatory set is exactly the set needed to (a) identify a
patient uniquely, (b) search for them by every key in FR-03 (insurance number,
date of birth, phone number, partial first/last name), and (c) decide billing.
Nothing else is forced, so registration does not invent data the clinic may
not have collected.

---

## A2. Scheduling constraints beyond professional availability (answers §6, bullet 2)

**Question:** What scheduling constraints apply beyond professional availability?

**Decision.** An appointment may move to status `Confirmed` only if **all**
of the following hold (FR-10, FR-11). Failure of any one is a distinct
extension in UC-02 (file 03).

1. **Working schedule:** the requested time falls inside a `Schedule` block
   during which that `HealthcareProfessional` practises at that
   `ClinicLocation` (FR-07, FR-08). Availability outside a scheduled block
   does not exist — there is no “free but not working” time.
2. **No professional double-booking:** the professional has no other
   appointment in status `Requested`, `Confirmed`, `CheckedIn` or
   `InConsultation` overlapping the requested interval.
3. **No patient double-booking:** the patient has no other overlapping
   appointment (with any professional) in one of those same statuses.
4. **Slot duration matches service type:** standard consultation = 30 minutes;
   initial specialist consultation = 60 minutes; nursing service = 15 minutes.
   A request whose duration does not match its service type is rejected, not
   silently adjusted.
5. **Not in the past, not too far ahead:** start time must be in the future
   and no more than **90 days** ahead.
6. **Location consistency:** professional, schedule block and appointment must
   all name the same `ClinicLocation`.
7. **Referral precondition:** if the service requires a referral (A3), a valid
   referral (A4) must exist before confirmation (FR-12).

**Rationale:** FR-10 (“verify availability”) and FR-11 (“verify applicable
scheduling constraints”) are separate requirements, so availability cannot be
the whole check. Constraints 2–6 are the minimum set that makes FR-11
testable without inventing clinic policy the brief never mentions (e.g. no
cancellation fees, no overbooking allowance).

---

## A3. Which services require a referral (answers §6, bullet 3)

**Question:** Which medical services require referrals?

**Decision** (FR-12, FR-21):

- **Referral required:** a *first* appointment with a **Specialist Physician**
  for a given specialty, where the patient has no currently valid referral
  (A4) for that specialty / that specialist.
- **Referral not required:** appointments with a Family Physician; nursing
  services; administrative services; and follow-up appointments that fall
  inside the validity period of an existing valid referral for the same
  specialty (see A4, rule 5).
- The referring professional for a required referral may be internal to the
  clinic **or external** (FR-23); see file 05.

**Rationale:** This mirrors the system description: “Depending on the service,
an appointment may require a medical referral (consultation request) from
another physician.” Specialist access is the example used throughout the
brief, and family medicine is presented as the usual *source* of referrals,
so requiring a referral for family medicine would be circular.

---

## A4. Referral validity conditions (answers §6, bullet 4)

**Question:** What conditions determine whether a referral is valid?

**Decision.** A `Referral` (synonym in the brief: consultation request) is
**valid for booking** if and only if all of the following hold:

1. `status` is `Submitted` or `Accepted` (see A7). `Draft`, `Rejected`,
   `Expired`, `Fulfilled` and `Cancelled` referrals are never valid.
2. **Not expired:** `currentDate <= expiryDate`, where
   `expiryDate = issuedDate + 12 months` unless the referring professional
   set a shorter explicit validity at creation.
3. **Patient match:** `referral.patient = appointment.patient`.
4. **Target match:** `referral.requestedSpecialty` equals the specialty of
   the requested appointment, **or** `referral.requestedProfessional` is the
   professional requested for the appointment.
5. **Not exhausted by status, not single-use:** a valid referral may support
   **one or more** appointments (FR-24). Booking an appointment against a
   referral does **not** consume it; it becomes `Fulfilled` only under A7.
6. **Clinical reason present:** `clinicalReason` is non-empty (a referral
   created without one cannot exist — see UC-08 extension 3a, file 05).

**Rationale:** Each rule is checkable from attributes that FR-22 already
requires the system to store (referrer, requested specialty/professional,
reason, priority, status) plus two dates any referral workflow needs
(`issuedDate`, `expiryDate`). No rule depends on information outside the system.

---

## A5. Who may generate invoices and register payments (answers §6, bullet 5)

**Question:** Which user roles may generate invoices and register payments?

**Decision** (FR-25–FR-27, FR-28, FR-29):

| Action | Administrative Staff | Healthcare Professional | Patient |
|---|---|---|---|
| Record the fee basis for an uninsured service (service rendered + fee) | No | Yes — as part of completing a consultation | No |
| Generate an `Invoice` (UC-12) | **Yes — only this role** | No | No |
| Register a `Payment` against an invoice (UC-13) | **Yes — only this role** | No | No (patient pays; staff register it) |
| View an invoice / payment | Yes | Only for own patients' services, read-only | Own invoices only |

Only uninsured services are invoiced (FR-25). RAMQ-covered and
private-insurance services do not generate a patient invoice in Iteration 1;
electronic RAMQ claim submission is explicitly out of scope (CON-03).

**Rationale:** Separating “who delivered / priced the service” from “who bills
for it” is exactly the role separation FR-29 demands for clinical vs.
administrative information, applied to money.

---

## A6. Activity log contents (answers §6, bullet 6)

**Question:** What information must be recorded in the activity log?

**Decision.** Every system operation that reads or changes patient,
appointment, consultation, referral, invoice or payment data creates one
`ActivityLog` entry (FR-30, NFR-02) containing:

- `logId` (unique, system-generated)
- `timestamp` (date + time of the operation)
- `actorUserId` and `actorRole` (who did it — never just “the system” when a
  user initiated it)
- `operationName` (the system operation, using the names in files 03–06)
- `entityType` and `entityId` affected (e.g. `Appointment`, its ID)
- `patientId`, **whenever patient-related data was accessed or changed**
  (this is what makes confidentiality auditing possible, NFR-01)
- `outcome`: `Success`, `Failure` or `AccessDenied`, with a short
  `failureReason` when not `Success`
- `sessionId` (CLI session in which the operation occurred)

**Explicitly not logged:** clinical content itself (observations, diagnoses,
referral reasons). The log records *that* clinical data was recorded, by whom,
for which patient — not *what it says*. Logging the content would recreate the
confidentiality risk the log exists to control.

Failed and denied attempts are logged too, not only successes — otherwise
unauthorized-access attempts (files 04, extension 4a) would be invisible.

**Rationale:** This is the smallest log that satisfies FR-30 (“record relevant
activities to support accountability and protection of confidentiality”) and
can answer the audit question: *who accessed or changed what patient's data,
when, and with what result?*

---

## A7. Status values — appointments and referrals (answers §6, bullet 7)

**Question:** What appointment and referral status values are supported?

### A7.1 Appointment statuses — `Appointment.status`

Canonical vocabulary, used identically in files 01–06:

| Status | Meaning | Entered when |
|---|---|---|
| `Requested` | Appointment asked for, checks not yet all passed / not yet confirmed | `requestAppointment(...)` succeeds (file 03) |
| `Confirmed` | All A2 checks + A4 referral check (if applicable) passed; slot is reserved | `confirmAppointment(...)` |
| `CheckedIn` | Patient's arrival registered on the day (UC-05 / FR-16) | Arrival registration |
| `InConsultation` | Consultation in progress | `startConsultation(...)` (file 04) |
| `Completed` | Consultation finished | `completeConsultation(...)` (file 04) |
| `Cancelled` | Appointment cancelled (UC-04); slot released | Cancellation |
| `NoShow` | Patient did not arrive and was not cancelled | End of appointment day, staff action |

Notes:
- **Rescheduling (UC-03) is an operation, not a status.** A rescheduled
  appointment keeps its identity, receives a new slot, and returns to
  `Confirmed`; the change is visible in the activity log (A6), not in a
  `Rescheduled` status. (Alternative designs use a status — this draft does
  not, and says so, so the model and scenarios agree.)
- Legal forward path:
  `Requested → Confirmed → CheckedIn → InConsultation → Completed`, with
  `Cancelled` reachable from `Requested`/`Confirmed`, and `NoShow` reachable
  from `Confirmed`.

### A7.2 Referral statuses — `Referral.status`

| Status | Meaning | Entered when |
|---|---|---|
| `Draft` | Created but not yet submitted; not valid for booking (A4) | `createReferral(...)` (file 05) |
| `Submitted` | Sent for the requested specialty/professional; valid for booking (A4) | `submitReferral(...)` (file 05) |
| `Accepted` | Requested professional/specialty accepted it; valid for booking | Acceptance by requested side |
| `Rejected` | Requested side declined it; never valid | Rejection by requested side |
| `Expired` | `expiryDate` (A4) passed before fulfilment; never valid | System / staff action after expiry date |
| `Fulfilled` | At least one appointment created from this referral has reached `Completed`, and no further appointments are planned under it | Staff/professional action after that completion |
| `Cancelled` | Withdrawn by the referring professional before fulfilment | Cancellation by referrer |

Legal forward paths:
`Draft → Submitted → Accepted → Fulfilled`, with `Rejected` reachable from
`Submitted`, `Expired` reachable from `Submitted`/`Accepted`, and `Cancelled`
reachable from `Draft`/`Submitted`/`Accepted`.

### A7.3 Referral priority values — `Referral.priority`

`Routine`, `Urgent`, `Emergent`. Priority affects ordering / how quickly the
clinic should act; it does **not** change referral validity (A4) or bypass any
scheduling constraint (A2).

---

## A8. Additional explicit assumptions (needed for consistency, not listed in §6)

- **A8.1 System boundary.** The ATS is the software system only (CLI, CON-01).
  RAMQ, pharmacies and provincial systems are outside the boundary and are not
  actors in Iteration 1 diagrams (CON-03).
- **A8.2 One clinic system, many locations.** All `ClinicLocation` records
  belong to the same ATS installation; a professional may practise at several
  of them (FR-08), expressed through their `Schedule` blocks (A2, rule 1).
- **A8.3 External referring professionals are data, not users.** An external
  physician (FR-23) does not log in to the ATS. Their referral is entered into
  the system by clinic staff / an internal professional on their behalf, with
  the external physician recorded as `referringProfessional` and flagged
  `isExternal = true` (see file 05). Only internal professionals are
  `SystemUser`s.
- **A8.4 Time granularity.** Appointment times are recorded to the minute;
  all durations in A2 rule 4 are multiples of 15 minutes.
- **A8.5 Search behaviour.** Patient search (FR-03 / UC-09) by partial first
  and/or last name is case-insensitive substring matching; search by insurance
  number, date of birth or phone number is exact matching. Multiple matches
  return a list for the actor to select from — the system never guesses which
  patient was meant (see UC-02 extension 7a, file 03).

## Cross-reference: where each assumption is used

| Assumption | Used in |
|---|---|
| A1 (mandatory fields, uniqueness) | 02 (Patient attributes/constraints), 03 ext. 7a, 05 ext. 5a |
| A2 (scheduling constraints) | 01 (Verify Scheduling Constraints), 02, 03 (MSS + ext. 4a–4c, contracts) |
| A3 (referral-required services) | 01 (Verify Referral Requirement), 03, 05 |
| A4 (referral validity) | 03 (ext. 5a–5c, `requestAppointment` contract), 05 |
| A5 (billing roles) | 01 (UC-12/UC-13 actors), 02 (Invoice/Payment), 04 (fee basis) |
| A6 (activity log) | 02 (ActivityLog), 03–05 (logging postconditions), 06 |
| A7 (status vocabularies) | 01, 02 (status attributes), 03–06 (every postcondition) |
| A8 (boundary, external referrers, search) | 01 (boundary/actors), 02, 03, 05 |
