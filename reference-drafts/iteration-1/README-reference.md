# Iteration 1 — Reference Drafts (for comparison / learning)

**Course:** SOEN 342 — Software Requirements and Deployment, Fall 2026
**Project:** Appointment Tracking System (ATS) for clinics
**Based on:** `docs/iteration-1/requirement.md` in the SOEN342-project repo
(actors, FR-01–FR-30, UC-01–UC-14, §6 open questions, §5.3 selected use cases)
and the Iteration 1 spec (`iter1.pdf`, posted Oct 6 2026, due Fri Oct 23 2026
23:59, weight 20%).

> **What these files are — and are not.**
> These are *reference drafts* written so Arham can compare them, section by
> section, against his own Iteration 1 work and learn from the differences.
> They are **not** a submission: the course rules in the iteration documents
> state that AI use is not allowed for the graded work, and Arham has stated
> he will not submit this material. Nothing in this folder was committed or
> pushed to GitHub, and the cloned repository was not edited. Use these files
> to check your own diagrams, scenarios and contracts for completeness and
> consistency — then write your final artifacts yourself, in your own words
> and drawings, so you can explain and defend every line in the final
> presentation.

---

## Index

| File | Contents in one line |
|---|---|
| `00-assumptions-and-open-questions.md` | Assumptions A1–A8 resolving every §6 open question; canonical appointment/referral status and priority vocabularies used by all other files. **Read this first — everything else depends on it.** |
| `01-use-case-diagram.md` | Critical (UC-01–UC-08) and non-critical (UC-09–UC-14) tables with actors, a full PlantUML use-case diagram, and a justification for every `<<include>>` / `<<extend>>`. |
| `02-domain-model.md` | Full PlantUML conceptual class diagram (Patient, coverage, professionals + subtypes, Specialty, ClinicLocation, Schedule, TimeSlot, Appointment, Consultation, ClinicalRecord, Referral, Prescription, Medication, Invoice, Payment, SystemUser/Role, ActivityLog), multiplicity explanations, FR traceability. |
| `03-uc02-book-appointment.md` | UC-02 fully dressed (MSS + 4 failure/alternative extensions), PlantUML SSDs for success **and** failure, contracts CO-02-1…CO-02-4. |
| `04-uc06-conduct-consultation.md` | UC-06 fully dressed (incl. UC-07), including the not-checked-in and unauthorised-actor failures, two SSDs, contracts CO-06-1…CO-06-6. |
| `05-uc08-create-medical-referral.md` | UC-08 fully dressed, including missing-reason, invalid-specialty/professional and external-referrer cases, two SSDs, contracts CO-08-2…CO-08-4 (plus shared CO-02-1). |
| `06-system-operations-and-contracts.md` | Consolidated list of all **13** system operations from UC-02/06/08 with source UC + SSD + contract ID, a summary contract table, and the cross-file consistency checks. |
| `README-reference.md` | This index. |

Suggested reading order: 00 → 01 → 02 → 03 → 04 → 05 → 06.
That is also the dependency order: assumptions → diagram → model → scenarios →
consolidation.

---

## How these files map to the Iteration 1 deliverables checklist

Deliverables list is quoted from `iter1.pdf` (“Deliverables (Artifacts)”).

| Iteration 1 deliverable (iter1.pdf) | Covered by | Notes / what a final submission still needs |
|---|---|---|
| “Use-case diagram, including critical and non-critical use-cases.” | `01` (+ `00` A3, A8 for actors/boundary) | The PlantUML source must be rendered (or redrawn in a UML tool) into a diagram image/document for submission — auto-generation caveats in the course docs are yours to observe; the *content* (actors, UC-01–UC-14, includes/extends) is fully specified in `01`. |
| “UML domain model” | `02` (+ `00` A1, A7 for attributes/statuses) | Same rendering note. Attributes, multiplicities, generalisations and FR traceability are all specified, so a redraw should be mechanical. |
| “System sequence diagram(s) to capture success and failure scenarios for critical use-cases.” | `03` §2, `04` §2, `05` §2 | Two SSDs per selected critical use case (one success, one failure) = 6 SSDs, in PlantUML sequence-diagram source with the system as a black box. Failure cases: UC-02 referral invalid/expired, UC-06 patient not checked in, UC-08 missing clinical reason — additional failures are described in the scenarios and summarised in SSD notes. |
| “Three fully-dressed scenarios for three critical use-cases, with at least one success and one failure case for each (an equivalent system sequence diagram to be provided in the above).” | `03` Part 1, `04` Part 1, `05` Part 1 | The three use cases are exactly the ones requirement.md §5.3 selects: **UC-02 Book Appointment, UC-06 Conduct Consultation, UC-08 Create Medical Referral**. Each file gives a Larman fully dressed template, a numbered main success scenario, and multiple numbered extensions (failures), plus the matching SSDs in the same file. |
| “Identification of system operations and operation contracts.” | `03` Part 3, `04` Part 3, `05` Part 3, consolidated in `06` | 13 distinct operations; full Larman contracts (operation, cross-references, preconditions, success + failure postconditions) live with their use case, and `06` is the single consolidated index + summary table. |
| *(Foundation, not a listed deliverable)* requirements / open questions resolved consistently | `00`; source = repo `requirement.md` | iter1.pdf expects assumptions to be resolved “consistently across artifacts” (requirement.md §6). `00` is where that consistency is anchored (statuses A7 are the highest-risk example). |

---

## Consistency conventions used across all files (worth copying as a *method*)

1. **One vocabulary for statuses** (draft 00, A7) — every postcondition in
every contract uses exactly those words.
2. **One name per operation** — draft 06's table character-matches the SSD
message labels; `searchPatient(...)` has one contract (CO-02-1) shared by
three use cases instead of three copies.
3. **SSDs show only actor↔system messages** — internal verifications
(availability, scheduling constraints, referral validity) are SSD *notes*
inside `requestAppointment(...)`, not fake actor messages.
4. **Every failure states its end state** — what was *not* created, what status
was *unchanged*, and that the failure/denial was logged (assumption A6).
5. **Traceability is written down, not implied** — each file names the FRs,
assumption IDs and cross-file references it depends on, so a grader (or you,
at presentation time) can follow any claim back to its source.
