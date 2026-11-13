# Iteration II — Design and Deployment

**Due:** Thursday, November 19, 2026, 23:59.

| Deliverable (Iteration II brief) | File |
|---|---|
| Impact of the new requirements (waiting lists, resources, PoC features) and the assumptions they need | `00-impact-analysis-and-assumptions.md` |
| Updated use-case diagram, including critical and non-critical use cases | `01-use-case-diagram.md` |
| Updated UML domain model | `02-domain-model.md` |
| Updated SSDs | `03-system-sequence-diagrams.md` |
| UML interaction diagrams for at least 3 critical use cases | `04-interaction-diagrams.md` (4 use cases, 5 operations) |
| UML class diagram (partial design model) | `05-design-class-diagram.md` |
| Proof of concept | `src/main/java/ats/` — see the repository `README.md` for how to run it |

Read in order 00 → 05: each file builds on the one before.

## Diagrams

All diagrams are written as PlantUML source in the Markdown files. To get
images, paste a code block into <https://www.plantuml.com/plantuml> or use a
PlantUML plugin in your editor, then add the image next to the source as was
done for Iteration I.

## How the pieces stay consistent

1. **One set of operation names.** The system operations in the SSDs (03 §0.2)
   are the public methods of the controllers in the code, and the messages in
   the interaction diagrams (04) are real method calls. The design class
   diagram (05) was written from the code.
2. **One set of assumptions.** B1–B10 (00) are used everywhere in this
   folder; Iteration I's A1–A8 still apply.
3. **The Iteration I booking fix carries through.** A slot is booked only when
   an appointment is confirmed. The code enforces it (`Appointment.confirm()`
   is the only caller of `TimeSlot.book()`), the tests check it, and the
   waiting list relies on it (B3).
4. **Scope is stated.** Waiting lists and resources are analysed and modelled
   (01–03) but, as the brief suggests, designed and built in Iteration III.
   Each place that leaves something for later says so.
