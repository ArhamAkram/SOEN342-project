# Iteration II — Updated System Sequence Diagrams

**Deliverable:** Updated SSDs.
**Conventions (unchanged from Iteration I):** the system is one black-box
lifeline; only actor↔system messages are system operations; internal checks
are notes. Every operation that changes data is logged (A6), which is not
repeated on every diagram.

The Iteration I SSDs for UC-06 and UC-08 are unchanged. UC-02's are updated in
§1; all others are new.

---

## 0. System operations — what changed and what is new

### 0.1 Changes to Iteration I operations

| Iteration I operation | Iteration II operation | Why |
|---|---|---|
| `requestAppointment(patientId, professionalId, locationId, serviceType, slotId, referralId [0..1])` | `requestAppointment(patientId, slotId, channel)` | The slot already identifies its professional and location, so passing them again could only create contradictions. The system looks up the patient's valid referral itself (A4) instead of the actor typing its ID. `channel` (`InPerson` / `Phone`) records how the booking was made (PoC: “a patient that has arrived in a clinic, or over the phone”). `serviceType` returns with resource allocation in Iteration III. |
| `confirmAppointment(appointmentId)` | unchanged signature | New failure reason `ResourceUnavailable` (B6). When the booking comes from an accepted waiting-list offer, success also marks the entry `Booked` (B3). |
| `cancelAppointment(appointmentId)` *(UC-04, not specified in Iteration I)* | `cancelAppointment(appointmentId)` | Returns whether a slot was released and, if so, the eligible waiting-list entries (B2). |

### 0.2 All system operations in these SSDs

| # | Operation | Use case | In PoC? |
|---|---|---|---|
| 1 | `searchPatient(searchCriteria)` | UC-02, UC-19 | ✔ |
| 2 | `registerPatient(firstName, lastName, telephone, ramqNumber [0..1])` | UC-01 (from UC-02) | ✔ |
| 3 | `getProfessionalAvailability(professional [0..1], specialty [0..1], fromDate, toDate)` | UC-02 | ✔ |
| 4 | `requestAppointment(patientId, slotId, channel)` | UC-02, UC-20 | ✔ |
| 5 | `confirmAppointment(appointmentId)` | UC-02, UC-20 | ✔ |
| 6 | `cancelAppointment(appointmentId)` | UC-04 | ✔ (without the waiting list) |
| 7 | `registerProfessional(firstName, lastName, title, specialty)` | UC-15 | ✔ |
| 8 | `makeAvailability(professionalId, fromDate, toDate, pattern, startTime, endTime, slotLengthMinutes)` | UC-15 | ✔ |
| 9 | `searchAvailability(professionalId [0..1], specialty [0..1], fromDate, toDate, onlyFree)` | UC-16 | ✔ |
| 10 | `importAvailability(sourceFile)` | UC-17 | ✔ |
| 11 | `importAppointments(sourceFile)` | UC-17 | ✔ |
| 12 | `exportAvailability(targetFile)` | UC-18 | ✔ |
| 13 | `addToWaitingList(patientId, professionalId [0..1], specialty [0..1], earliestDate [0..1], latestDate [0..1], priority [0..1])` | UC-19 | Iteration III |
| 14 | `getEligibleEntries(slotId)` | UC-20 | Iteration III |
| 15 | `recordOfferOutcome(entryId, slotId, outcome)` | UC-20 | Iteration III |
| 16 | `getWaitingList(professionalId [0..1], specialty [0..1])` | UC-21, UC-22 | Iteration III |
| 17 | `changePriority(entryId, newPriority)` | UC-21 | Iteration III |
| 18 | `removeFromWaitingList(entryId, reason)` | UC-22 | Iteration III |

The operation names 1–12 are the method names of the PoC controllers
(`05-design-class-diagram.md`), so the SSDs, interaction diagrams and code use
one vocabulary.

---

## 1. UC-02 Book Appointment (updated)

### 1.1 Success — patient at the clinic or on the phone

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : searchPatient(searchCriteria)
System --> Actor : matchingPatients

opt patient not registered
  Actor -> System : registerPatient(firstName, lastName, telephone, ramqNumber)
  System --> Actor : patient(patientId)
end

Actor -> System : getProfessionalAvailability(professional, specialty, fromDate, toDate)
System --> Actor : freeSlots [free, in the future]

Actor -> System : requestAppointment(patientId, slotId, channel)
note right of System
  Checks: slot free, not in the past,
  no overlapping appointment for the patient,
  valid referral if the professional is a specialist.
  Creates Appointment (status = Requested).
  The slot is NOT booked.
end note
System --> Actor : appointment(appointmentId, status = Requested)

Actor -> System : confirmAppointment(appointmentId)
note right of System
  Re-runs the same checks, plus resources (B6,
  Iteration III). Books the slot; status = Confirmed.
end note
System --> Actor : appointment(appointmentId, status = Confirmed)
@enduml
```

`channel` is `InPerson` when the patient is at the desk and `Phone` when staff
book over the phone; the flow is the same. A patient booking for themselves
follows the same messages.

### 1.2 Failure — no slot available (extends to UC-19)

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : searchPatient(searchCriteria)
System --> Actor : matchingPatients

Actor -> System : getProfessionalAvailability(professional, specialty, fromDate, toDate)
System --> Actor : freeSlots = [] (empty)

note over Actor, System
  No appointment is requested and nothing is created.
  If the patient wants, staff continue with UC-19
  Add Patient to Waiting List (see §6).
  Other UC-02 failures (ReferralRequired, booked slot,
  confirmation failing) are as in Iteration I.
end note
@enduml
```

---

## 2. UC-15 Make Availability

### 2.1 Success — “during November, Dr. X is available every Monday, 9 to 12”

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

opt professional not yet in the system
  Actor -> System : registerProfessional(firstName, lastName, title, specialty)
  System --> Actor : professional(professionalId)
end

Actor -> System : makeAvailability(professionalId, 2026-11-01, 2026-11-30,\n  Weekly{MONDAY}, 09:00, 12:00, 30)
note right of System
  For each date in the range where the pattern occurs,
  cut 09:00-12:00 into 30-minute slots and create them.
  Slots overlapping existing ones are skipped (B7).
end note
System --> Actor : result(created = 30 slots, skipped = [])
@enduml
```

### 2.2 Failure — invalid slot length; partial overlap

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : makeAvailability(professionalId, fromDate, toDate,\n  pattern, 09:00, 10:00, 45)
note right of System
  45 does not divide the 60-minute block.
  Nothing is created.
end note
System --> Actor : rejected("Slot length must divide the block evenly")

Actor -> System : makeAvailability(professionalId, fromDate, toDate,\n  pattern, 09:00, 10:00, 30)
note right of System
  09:00-09:30 already exists on one date:
  that slot is skipped, the rest are created.
end note
System --> Actor : result(created = n, skipped = [date 09:00: overlaps S00001])
@enduml
```

---

## 3. UC-16 Search Availability

```plantuml
@startuml
actor "Administrative Staff\n(or Patient)" as Actor
participant ": ATS (System)" as System

Actor -> System : searchAvailability(professionalId, specialty, fromDate, toDate, onlyFree)
note right of System
  Read-only. Professional and specialty
  are optional filters (B10).
end note
System --> Actor : slots [in date and time order]
@enduml
```

An empty result is a normal answer, not a failure; there is no separate
failure SSD. An unknown `professionalId` is rejected with nothing changed.

---

## 4. UC-17 Import Data from CSV

### 4.1 Success

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : importAvailability(sourceFile)
note right of System
  For each row: find or create the professional,
  create the TimeSlot (B8).
end note
System --> Actor : importSummary(imported = 20, skipped = [])

Actor -> System : importAppointments(sourceFile)
note right of System
  For each row: find the professional, find or
  register the patient, find or create the slot,
  record the appointment as Confirmed (B8).
end note
System --> Actor : importSummary(imported = 5, skipped = [])
@enduml
```

### 4.2 Failure — unreadable file; bad rows

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : importAvailability(sourceFile)
System --> Actor : error("Could not read file")
note right of System : Nothing is imported.

Actor -> System : importAppointments(sourceFile)
note right of System
  Bad rows are skipped; good rows are
  still imported (B8).
end note
System --> Actor : importSummary(imported = n,\n  skipped = ["line 4: unknown professional", ...])
@enduml
```

---

## 5. UC-18 Export Availability to CSV

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : exportAvailability(targetFile)
note right of System
  Writes every slot with a Status column
  (Available / Booked). Read-only for the
  model (B9).
end note
System --> Actor : exportSummary(slotsWritten)
@enduml
```

---

## 6. UC-19 Add Patient to Waiting List

### 6.1 Success

```plantuml
@startuml
actor "Administrative Staff\n(or Patient)" as Actor
participant ": ATS (System)" as System

Actor -> System : searchPatient(searchCriteria)
System --> Actor : matchingPatients

Actor -> System : getProfessionalAvailability(professional, specialty, fromDate, toDate)
System --> Actor : freeSlots = [] (empty)

Actor -> System : addToWaitingList(patientId, professionalId, specialty,\n  earliestDate, latestDate, priority)
note right of System
  Exactly one of professionalId / specialty (B1).
  Creates WaitingListEntry: status = Waiting,
  requestedOn = now, priority (default 3, B4).
end note
System --> Actor : entryAdded(entryId, priority)
@enduml
```

### 6.2 Failure — a slot is available; invalid target

```plantuml
@startuml
actor "Administrative Staff\n(or Patient)" as Actor
participant ": ATS (System)" as System

Actor -> System : addToWaitingList(patientId, professionalId, specialty,\n  earliestDate, latestDate, priority)
note right of System
  A free slot matches the request (B1),
  or both / neither of professionalId and
  specialty were given, or the patient
  already waits for this target.
  No entry is created; the failure is logged.
end note
System --> Actor : rejected(reason = SlotAvailable /\n  InvalidTarget / AlreadyWaiting)
@enduml
```

---

## 7. UC-20 Offer Freed Slot to Waitlisted Patient (extends UC-04)

### 7.1 Success — the first eligible patient accepts

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : cancelAppointment(appointmentId)
note right of System
  The appointment was Confirmed, so its slot
  is released (B2) and eligible entries found.
end note
System --> Actor : cancelled(slotReleased = true, slotId)

Actor -> System : getEligibleEntries(slotId)
System --> Actor : eligibleEntries [by priority, then requestedOn]

note over Actor : Staff phone the first patient on the list.\nThe patient accepts.

Actor -> System : recordOfferOutcome(entryId, slotId, Accepted)
System --> Actor : offerRecorded(offerId)

Actor -> System : requestAppointment(patientId, slotId, Phone)
System --> Actor : appointment(appointmentId, status = Requested)

Actor -> System : confirmAppointment(appointmentId)
note right of System
  Normal UC-02 checks (referral, overlap...).
  Books the slot. Because this patient has an
  Accepted offer for this slot, the offer is
  linked to the appointment and the entry
  becomes Booked (B3).
end note
System --> Actor : appointment(appointmentId, status = Confirmed)
@enduml
```

### 7.2 Failure — the patient declines and is removed; the slot was taken meanwhile

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : getEligibleEntries(slotId)
System --> Actor : eligibleEntries

note over Actor : Staff phone the first patient. They decline.

Actor -> System : recordOfferOutcome(entryId, slotId, Declined)
System --> Actor : offerRecorded(offerId)

opt staff choose to remove the patient (UC-22)
  Actor -> System : removeFromWaitingList(entryId, Declined)
  System --> Actor : removed(entryId, status = Removed)
end

note over Actor : Staff phone the next patient. They accept.

Actor -> System : recordOfferOutcome(nextEntryId, slotId, Accepted)
System --> Actor : offerRecorded(offerId)
Actor -> System : requestAppointment(patientId, slotId, Phone)
System --> Actor : appointment(appointmentId, status = Requested)
Actor -> System : confirmAppointment(appointmentId)
note right of System
  Someone else booked the slot meanwhile
  (no slot is held during offers, B3).
  Appointment becomes Cancelled; the entry
  stays Waiting; failure logged (UC-02 ext. 9a).
end note
System --> Actor : rejected(reason = ProfessionalUnavailable)
@enduml
```

---

## 8. UC-21 Change Waiting-List Priority

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : getWaitingList(professionalId, specialty)
System --> Actor : entries [by priority, then requestedOn]

Actor -> System : changePriority(entryId, newPriority)
note right of System
  Entry must be Waiting; newPriority >= 1 (B4).
  Logged with old and new value.
  Otherwise rejected, nothing changed.
end note
System --> Actor : entryUpdated(entryId, priority = newPriority)
@enduml
```

## 9. UC-22 Remove Patient from Waiting List

```plantuml
@startuml
actor "Administrative Staff" as Actor
participant ": ATS (System)" as System

Actor -> System : getWaitingList(professionalId, specialty)
System --> Actor : entries

Actor -> System : removeFromWaitingList(entryId, reason)
note right of System
  status = Removed, removalReason set (B5).
  The entry is kept for the record, not deleted.
end note
System --> Actor : removed(entryId, status = Removed)
@enduml
```
