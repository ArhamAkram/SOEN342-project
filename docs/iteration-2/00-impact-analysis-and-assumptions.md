# Iteration II — Impact Analysis and New Assumptions

**System:** Appointment Tracking System (ATS) — SOEN 342, Fall 2026
**Input:** Iteration II brief (posted Oct 26 2026, due Nov 19 2026), plus the
Iteration I artifacts in `reference-drafts/iteration-1/` (as revised after the
Iteration I feedback).
**Rule carried over from Iteration I:** every later file in this folder uses
the decisions below word for word. Iteration I assumptions A1–A8 still hold
unless a B-assumption here says otherwise.

---

## 1. What the new requirements change

| New requirement (Iteration II brief) | What it changes | Where |
|---|---|---|
| **Waiting lists** — a patient may be placed on a waitlist for a professional, a specialty or a date range when no slot is available | New use case UC-19 *Add Patient to Waiting List* (extends UC-02 when no slot is found). New domain concept `WaitingListEntry`. | 01, 02, 03 |
| When an appointment is cancelled, the system identifies **eligible waitlisted patients** | UC-04 *Cancel Appointment* now has a consequence: releasing a confirmed slot triggers a search for eligible entries (B2). New use case UC-20 *Offer Freed Slot to Waitlisted Patient* extends UC-04. | 01, 03 |
| The slot is **offered by phone**, by priority; a patient who declines may be removed | New concept `SlotOffer` with its outcome. New use case UC-22 *Remove Patient from Waiting List* (also extends UC-20 when the patient declines). | 01, 02, 03 |
| **Priority** may be changed by administrative staff | New use case UC-21 *Change Waiting-List Priority*. `WaitingListEntry.priority`. | 01, 02, 03 |
| **Resource allocation** — some services need a room, equipment or an assisting role | `ServiceType` becomes a class (it was a string attribute of `Appointment`). New concepts `ResourceType`, `Resource`, `ResourceRequirement`, `ResourceAllocation`. New included behaviour *Verify Resource Availability* in UC-02. | 01, 02 |
| Human resources are **roles**, not named people | A role (e.g. *Assisting nurse*) is a `ResourceType` with a `capacity`, and has no `Resource` instances (B6). | 02 |
| PoC **Making Availability** (date range + repetition pattern + start/end) | New use case UC-15 *Make Availability*. `Schedule` now carries the date range, times, slot length and a `RecurrencePattern`; it generates `TimeSlot`s. | 01, 02, 03 |
| PoC **Searching for Availability** (directly or while booking) | New use case UC-16 *Search Availability*, included by UC-02 and usable on its own. | 01, 03 |
| PoC **Import from CSV** / **Export to CSV** | New use cases UC-17 *Import Data from CSV* and UC-18 *Export Availability to CSV*. | 01, 03 |
| PoC — the system starts empty, no database | No persistence classes in the design; registries are in memory (05). | 04, 05 |

**Unchanged by Iteration II:** UC-01, UC-03, UC-05 to UC-14 and their
operations. UC-02's operations keep the request-then-confirm split decided
after the Iteration I feedback, which the waiting list relies on (B3).

---

## 2. New assumptions (B1–B10)

### B1. Joining a waiting list
- A patient may be added only when the search for the requested target
  returns **no free slot** (the brief: “when a healthcare professional has no
  available appointment slots”).
- An entry names **exactly one target**: a specific professional **or** a
  specialty. It may also give a date range (`earliestDate`, `latestDate`,
  each optional). A date range alone, with no professional or specialty, is
  not accepted: it would match every slot in the clinic.
- An entry starts with `status = Waiting`, `requestedOn` = now, and a
  `priority` (B4). The same patient may have several entries for different
  targets, but not two `Waiting` entries for the same target.
- Joining the list never books or holds anything.

### B2. Eligibility when a slot is freed
A slot is “freed” only when a **confirmed** appointment is cancelled (a
`Requested` appointment holds no slot — Iteration I fix, A7.1). An entry is
**eligible** for that slot when all of these hold:
1. `status = Waiting`;
2. its target matches: the same professional, or the slot's professional
   holds the requested specialty;
3. the slot's date is inside the entry's date range (when one is given);
4. the slot is still free and in the future;
5. the patient has no overlapping slot-occupying appointment (A2 rule 3).

Eligible entries are listed in **priority order** (1 = highest), ties broken
by `requestedOn` (earliest first). The referral rule (A3/A4) is **not** used to
filter the list; it is checked when the offer is accepted, so staff see the
patient and can arrange a referral if one is missing.

### B3. Offering the slot (by phone)
- Staff phone the first eligible patient and record a `SlotOffer` with outcome
  `Accepted`, `Declined` or `NoAnswer`.
- **The slot is not held while staff phone people.** This keeps the
  Iteration I rule that only confirmation books a slot.
- `Accepted`: the booking is made with the normal UC-02 operations
  (`requestAppointment` then `confirmAppointment`), so every A2/A3/A4 check
  still applies. If confirmation fails because someone else booked the slot
  in the meantime, UC-02 extension 9a applies and the entry stays `Waiting`.
  On success the entry becomes `Booked`.
- `Declined`: staff may remove the patient (UC-22), or leave them `Waiting`.
- `NoAnswer`: the entry stays `Waiting`; staff move on to the next patient.

### B4. Priority
`priority` is a positive whole number; **1 is the highest**. A new entry gets
priority 3 (normal) unless staff choose another. Only Administrative Staff
may change it (UC-21). Every change is logged with the old and new value.

### B5. Removal
Entries are never deleted, so the history stays auditable (FR-30): removal
sets `status = Removed` and records a `removalReason` (`Declined`,
`PatientRequest`, `NoLongerNeeded`, `BookedElsewhere`).

### B6. Resources
- The clinic's resources come from a **predefined list** (the brief) loaded
  at start-up; no use case creates or deletes them.
- A `ResourceType` has a `kind`: `Room`, `Equipment` or `Role`.
- Rooms and equipment are individual `Resource`s (e.g. *Exam Room 2*,
  *ECG machine #1*).
- **Roles** (e.g. *Assisting nurse*) are not tied to a person (the brief).
  A role has a `capacity` — how many can be busy at the same time — and has no
  `Resource` instances.
- Each `ServiceType` lists what it needs as `ResourceRequirement`s (a
  resource type and a quantity).
- Resources are allocated **when an appointment is confirmed**, for the slot's
  time, and released when it is cancelled — the same moment the slot is
  booked or released. If a required resource is not free,
  `confirmAppointment` fails with the new reason `ResourceUnavailable`.
- As the brief suggests, the *design* of resource allocation is left for
  Iteration III; the PoC does not allocate resources.

### B7. Making availability
- Staff give a professional, a date range, a repetition pattern, a start and
  end time, and a slot length.
- Patterns: **weekly** on chosen days (e.g. every Monday), **monthly on a
  date** (e.g. the 15th), or **monthly on a weekday** (e.g. the first
  Tuesday).
- The block start–end is cut into slots of the given length, which must
  divide the block evenly. A length of 0 means “one slot for the whole block”.
- A slot that would overlap an existing slot of the same professional is
  **skipped and reported**, not created twice. The other slots are still
  created.

### B8. CSV import
- The system starts empty; import is how initial data is loaded.
- Two files are supported, in the formats given in the brief:
  *Professional Availability* (First Name, Last Name, Title, Specialty, Date,
  Start, End) and *Booked Appointments* (Professional, Date, Start, End,
  Patient, Telephone, RAMQ No.).
- The CSV has no professional ID, so a professional is identified by first
  and last name; a new name creates a new professional.
- A patient in the appointments file is matched by RAMQ number, then by
  telephone and name; otherwise they are registered. A dash means “no RAMQ
  number”.
- Booked appointments are existing bookings: they are recorded as `Confirmed`
  without re-running the booking checks (e.g. past dates, referrals).
- Bad rows are **skipped and reported with their line number**; good rows are
  still imported.
- The waiting-list sample file in the brief is not imported by the PoC (the
  waiting list is not part of the PoC); it is kept in `data/waitlist.csv` for
  Iteration III.

### B9. CSV export
Export writes **every** slot (booked or not) in the availability import format
plus a `Status` column (`Available` / `Booked`), so an exported file can be
imported again.

### B10. Search availability
Search takes an optional professional, an optional specialty and a date range.
Used on its own (UC-16) it may include booked slots; used while booking
(UC-02) it returns only free slots that start in the future.

---

## 3. Use-case numbering added in Iteration II

| ID | Use case | Primary actor | Critical? |
|---|---|---|---|
| UC-15 | Make Availability | Administrative Staff | Critical (PoC) |
| UC-16 | Search Availability | Administrative Staff, Patient | Critical (PoC) |
| UC-17 | Import Data from CSV | Administrative Staff | Critical (PoC) |
| UC-18 | Export Availability to CSV | Administrative Staff | Non-critical |
| UC-19 | Add Patient to Waiting List | Administrative Staff, Patient | Critical |
| UC-20 | Offer Freed Slot to Waitlisted Patient | Administrative Staff | Critical |
| UC-21 | Change Waiting-List Priority | Administrative Staff | Non-critical |
| UC-22 | Remove Patient from Waiting List | Administrative Staff | Non-critical |
