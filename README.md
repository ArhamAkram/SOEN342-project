# SOEN342-project

# SOEN 342 – Software Requirements and Deployment
**Fall 2026 – Team Project**

## Team Information

| Name | Student ID | GitHub Username | Role |
|------|------------|-----------------|------|
| [Arham Akram] | [40224890] | [@ArhamAkram] | Team Leader |
| [Rohit Sengupta] | [40247353] | [@reshinigami] | Member |
| [Bushr] | [ID] | [@username] | Member |

## Repository Structure
- `docs/` – Requirements, UML diagrams, OCL specifications
  - `docs/iteration-1/` – Iteration I requirements
  - `docs/iteration-2/` – Iteration II models and design (start with its `README.md`)
- `reference-drafts/iteration-1/` – Iteration I use cases, SSDs and contracts
- `src/main/java/ats/` – Source code (Java): the Iteration II proof of concept
- `src/test/java/ats/` – Unit tests (JUnit 5)
- `data/` – Sample CSV files from the Iteration II brief

## Running the proof of concept

Requires Java 17 or newer and Maven.

```bash
mvn test                      # build and run the unit tests
mvn package -DskipTests       # build target/appointment-tracking-system-0.2.0.jar
java -jar target/appointment-tracking-system-0.2.0.jar
```

Without Maven:

```bash
mkdir -p out && javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
java -cp out ats.Main
```

The system starts empty. Use menu option **1** with `data/availability.csv`,
then option **2** with `data/appointments.csv`, to load the sample data.
Nothing is saved between runs.

| Menu option | Feature (Iteration II PoC) |
|---|---|
| 1, 2 | Import from CSV (availability; booked appointments) |
| 3 | Export to CSV (the whole availability database) |
| 4 | Making availability (date range + weekly / monthly pattern + start and end time) |
| 5 | Searching for availability |
| 6 | Booking an appointment, for a patient at the clinic or on the phone |
| 7–12 | Cancel, list appointments, register patient, record referral, list professionals, activity log |

## Iterations
- [x] Iteration 0 – Team setup and GitHub environment
- [x] Iteration I – System Modelling and Requirements Specification
- [ ] Iteration II – Design and Deployment (in progress)
- [ ] Iteration III
