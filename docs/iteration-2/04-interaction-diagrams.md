# Iteration II — UML Interaction Diagrams

**Deliverable:** UML interaction diagrams for at least three critical use cases.
**Covered:** UC-02 Book Appointment (`requestAppointment`, `confirmAppointment`),
UC-15 Make Availability (`makeAvailability`), UC-16 Search Availability
(`searchAvailability`), and UC-17 Import Data from CSV (`importAvailability`).
That is four critical use cases and five system operations.

**How these were made.** Each diagram is the design of one system operation
from `03-system-sequence-diagrams.md`. Every message is a real method call in
the PoC (`src/main/java/ats/...`), with the same name and parameters, so the
diagrams, the design class diagram (`05`) and the code agree. Calls that only
read a value (simple getters) are shown only where they matter to a decision.
Activity-log calls are shown once per operation as `log.success(...)` /
`log.failure(...)`.

---

## 0. GRASP decisions used in all diagrams

| Decision | Pattern | Reason |
|---|---|---|
| The console (`ConsoleUI`) never changes domain objects; it calls one controller method per system operation | **Controller** (use-case controller) | Keeps the user interface separate from domain logic. One controller per group of related use cases: `BookingController` (UC-02, UC-04), `AvailabilityController` (UC-15, UC-16), `CsvController` (UC-17, UC-18). A single facade controller would grow too large (low cohesion). |
| `Clinic` creates the registries and the controllers | **Creator** | `Clinic` contains and aggregates them, and records the shared `Clock` and `ActivityLog` they need. |
| `AvailabilityCatalog` creates `TimeSlot`s and refuses overlapping ones | **Creator**, **Information Expert** | It contains all slots, so it is the only object that can know whether a new slot overlaps another. |
| `AppointmentBook` creates `Appointment`s and answers “does this patient already have an overlapping appointment?” | **Creator**, **Information Expert** | It records all appointments. |
| `Appointment.confirm()` books its own slot; `Appointment.cancel()` releases it | **Information Expert** | The appointment knows its status and its requested slot. Keeping the booking in one method enforces “only confirmation books a slot” (Iteration I fix). `TimeSlot.book()` / `release()` are package-private so nothing else can call them. |
| `Referral.isValidFor(patient, specialty, date)` | **Information Expert** | The referral holds the status, dates, patient and specialty that rules A4 1–4 test. |
| `HealthcareProfessional.isSpecialist()` | **Information Expert** | The professional knows their specialty (rule A3). |
| `RecurrencePattern.occursOn(date)` with three implementations | **Polymorphism**, **Protected Variations** | `makeAvailability` does not need to know which kind of pattern it has; a new kind of repetition is a new class, not a change to the controller. |
| CSV reading and writing are in `ats.csv` classes, not in the controllers | **Pure Fabrication**, **High Cohesion** | File formats are not domain concepts. Keeping them apart means a database (Iteration III) can replace them without touching domain classes. |
| Controllers depend on registries, not on each other | **Low Coupling** | Each controller can be tested on its own (see `src/test`). |

---

## 1. UC-02 Book Appointment — `requestAppointment(patientId, slotId, channel)`

```plantuml
@startuml
skinparam sequenceMessageAlign center
actor "Administrative Staff" as Staff
participant ":ConsoleUI" as UI
participant ":BookingController" as BC
participant "patients :\nPatientRegistry" as PR
participant "availability :\nAvailabilityCatalog" as AC
participant "appointments :\nAppointmentBook" as AB
participant "referrals :\nReferralRegistry" as RR
participant "slot : TimeSlot" as S
participant "prof :\nHealthcareProfessional" as HP
participant "log : ActivityLog" as LOG

Staff -> UI : choose slot
UI -> BC : requestAppointment(patientId, slotId, channel)
BC -> PR : patient = findById(patientId)
BC -> AC : slot = findById(slotId)

group verifyBookable(patient, slot)
  BC -> S : isBooked()
  BC -> S : getStartDateTime()
  BC -> AB : patientHasOverlap(patient, slot)
  BC -> S : prof = getProfessional()
  BC -> HP : isSpecialist()
  opt specialist
    BC -> RR : referral = findValid(patient, specialty, slot.date)
    note right of RR
      loop over referrals:
      r.isValidFor(patient, specialty, date)
    end note
  end
end

BC -> AB : a = create(patient, slot, channel, referral)
create participant "a : Appointment" as A
AB -> A : <<create>>(id, patient, slot, channel, referral)
note right of A : status = REQUESTED\nslot is NOT booked
BC -> LOG : success("requestAppointment", ...)
BC --> UI : a
UI --> Staff : "Requested: A00006 ..."

note over BC, LOG
  Any failed check throws BookingException(reason):
  BC calls log.failure("requestAppointment", reason)
  and nothing is created.
end note
@enduml
```

**Why this design.** `BookingController` coordinates but decides nothing on its
own: each check is asked of the object that holds the information
(`TimeSlot`, `AppointmentBook`, `HealthcareProfessional`, `ReferralRegistry`).
`AppointmentBook` creates the appointment because it records all appointments
(Creator). The checks live in one private method, `verifyBookable`, because
`confirmAppointment` must run exactly the same checks again.

---

## 2. UC-02 Book Appointment — `confirmAppointment(appointmentId)`

```plantuml
@startuml
participant ":ConsoleUI" as UI
participant ":BookingController" as BC
participant "appointments :\nAppointmentBook" as AB
participant "a : Appointment" as A
participant "slot : TimeSlot" as S
participant "log : ActivityLog" as LOG

UI -> BC : confirmAppointment(appointmentId)
BC -> AB : a = findById(appointmentId)
BC -> A : getStatus()
note right of BC : must be REQUESTED

BC -> BC : verifyBookable(a.patient, a.requestedSlot)
note right of BC : same checks as in requestAppointment (diagram 1)

alt all checks pass
  BC -> A : confirm()
  A -> S : book(a)
  note right of S : bookedBy = a\n(the only place a slot is booked)
  A -> A : status = CONFIRMED
  BC -> LOG : success("confirmAppointment", ...)
  BC --> UI : a
else a check fails (extension 9a)
  BC -> A : cancel()
  note right of A : REQUESTED -> CANCELLED;\nit held no slot, so nothing is released
  BC -> LOG : failure("confirmAppointment", reason)
  BC --> UI : BookingException(reason)
end
@enduml
```

**Why this design.** `Appointment.confirm()` is the Information Expert for its
own state change and books its own slot, so “confirm” and “book the slot”
cannot happen separately. This is the design form of the Iteration I contract
CO-02-4.

---

## 3. UC-15 Make Availability — `makeAvailability(professionalId, from, to, pattern, start, end, slotMinutes)`

```plantuml
@startuml
participant ":ConsoleUI" as UI
participant ":AvailabilityController" as AVC
participant "professionals :\nProfessionalRegistry" as PR
participant "pattern :\nRecurrencePattern" as P
participant "availability :\nAvailabilityCatalog" as AC
participant "log : ActivityLog" as LOG

UI -> AVC : makeAvailability(professionalId, from, to,\n  pattern, start, end, slotMinutes)
AVC -> PR : professional = findById(professionalId)
note right of AVC
  Validates: to >= from, end > start,
  slotMinutes divides the block (B7).
  Otherwise IllegalArgumentException, nothing created.
end note

loop for each date from 'from' to 'to'
  AVC -> P : occursOn(date)
  opt pattern occurs on date
    loop for t = start; t < end; t += step
      AVC -> AC : addSlot(professional, date, t, t + step)
      alt no overlap with that professional's slots
        create participant "s : TimeSlot" as TS
        AC -> TS : <<create>>(id, professional, date, t, t + step)
        AC --> AVC : s
        AVC -> AVC : created.add(s)
      else overlap
        AC --> AVC : IllegalArgumentException
        AVC -> AVC : skipped.add(date + t + reason)
      end
    end
  end
end

AVC -> LOG : success("makeAvailability", counts)
create participant ":AvailabilityResult" as R
AVC -> R : <<create>>(created, skipped)
AVC --> UI : result
@enduml
```

**Why this design.** `RecurrencePattern` is an interface with
`WeeklyPattern`, `MonthlyByDatePattern` and `MonthlyByWeekdayPattern`
(**Polymorphism**): the controller only asks `occursOn(date)`.
`AvailabilityCatalog` both creates slots and refuses overlaps (**Creator** +
**Information Expert**), so the overlap rule cannot be bypassed — the same
`addSlot` is used by the CSV import.

---

## 4. UC-16 Search Availability — `searchAvailability(professionalId, specialty, from, to, onlyFree)`

```plantuml
@startuml
participant ":ConsoleUI" as UI
participant ":AvailabilityController" as AVC
participant "professionals :\nProfessionalRegistry" as PR
participant "availability :\nAvailabilityCatalog" as AC
participant "s : TimeSlot" as S

UI -> AVC : searchAvailability(professionalId, specialty,\n  from, to, onlyFree)
opt professionalId given
  AVC -> PR : professional = findById(professionalId)
end
create participant "q : SlotQuery" as Q
AVC -> Q : <<create>>(professional, specialty, from, to, onlyFree)
AVC -> AC : search(q)
loop each slot s
  AC -> S : getProfessional(), getDate(), isBooked()
  note right of AC : keep s if it matches q
end
AC --> AVC : matching slots, sorted by date and time
AVC --> UI : slots
@enduml
```

**Why this design.** The search criteria travel as one `SlotQuery` object
rather than five loose parameters, so `AvailabilityCatalog.search` stays
stable if a criterion is added (e.g. resources in Iteration III). The catalog
does the filtering because it holds the slots (**Information Expert**).
While booking, `BookingController.getProfessionalAvailability` uses the same
`search` with `onlyFree = true` and then drops slots in the past.

---

## 5. UC-17 Import Data from CSV — `importAvailability(sourceFile)`

```plantuml
@startuml
participant ":ConsoleUI" as UI
participant ":CsvController" as CC
participant ":AvailabilityCsvImporter" as IMP
participant "CsvFormat" as F
participant "professionals :\nProfessionalRegistry" as PR
participant "availability :\nAvailabilityCatalog" as AC
participant "result : ImportResult" as R
participant "log : ActivityLog" as LOG

UI -> CC : importAvailability(sourceFile)
CC -> IMP : result = importFrom(reader)
create R
IMP -> R : <<create>>
loop each line of the file
  IMP -> F : fields = parseLine(line)
  alt header row
    note right of IMP : skipped
  else valid row
    IMP -> PR : prof = findOrCreate(first, last, title, specialty)
    IMP -> AC : addSlot(prof, date, start, end)
    IMP -> R : imported()
  else bad date/time, missing field or overlap
    IMP -> R : skipped(lineNumber, reason)
  end
end
CC -> LOG : success("importAvailability", summary)
CC --> UI : result
@enduml
```

**Why this design.** The CSV format is handled by **Pure Fabrication** classes
(`AvailabilityCsvImporter`, `CsvFormat`) so that no domain class knows about
files. The importer reuses `ProfessionalRegistry.findOrCreate` and
`AvailabilityCatalog.addSlot`, so imported data passes the same overlap rule
as availability made by hand. `importAppointments` follows the same shape with
`AppointmentCsvImporter`.
