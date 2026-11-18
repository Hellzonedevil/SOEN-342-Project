# SOEN-342-Project
Team Members: 
1) Sarvesh Sai Rajesh - 40231819 (Team Lead)
2) Alex Khoury - 40299086
3) Nicholas Nguyen - 40317237


## How to compile and run (Java 17 or newer)

Run these from the repository root.

**macOS / Linux / Git Bash**
```sh
javac -encoding UTF-8 -d out $(find code test -name "*.java")
java -cp out app.ClinicCLI
```

**Windows PowerShell**
```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse code,test -Filter *.java).FullName
java -cp out app.ClinicCLI
```

**Tests**
```sh
java -cp out booking.AvailabilityServiceTest
java -cp out csv.CsvImportExportTest
java -cp out app.ClinicCLITest
```

**Quick demo** (menu numbers in brackets): import `data/availability_sample.csv` [4] -> list professionals and services [6] ->
search availability for `PRO-1`, `SRV-1`, 2026-11-02 [2] -> book patient "Lavoie" at `2026-11-02 09:00` [3] ->
search again (09:00 is gone) [2] -> make weekly availability for a new professional [1] -> export to a file [5].
`data/availability_with_errors.csv` shows how bad rows are rejected by line number.

Here's my module (as member 1 from our task division) with the 3 C1 entry points in BookingService:
1. `searchPatients(criteria)` - filters by the identifying criteria that we documented in Iteration 1
2. `findAvailableSlots(professionalId, serviceId, interval)` - delegates to Member 2's availability module and excludes already booked appointments
3. `bookAppointment(patientId, professionalId, serviceId, startsAt, referralId)` - (no `endsAt`: the end time is `startsAt` + the service duration, per the Iteration 1 feedback) validates the references, rechecks the selected interval, checks required referrals, creates a `CONFIRMED` appointment and records a minimal booking audit event.


Extra Notes:
Iteration 2's new waitlist and predefined resource requirements are analysed (but not coded) by me, as Member #1, in the word document. The interation 2 instructions suggest deferring the design of these additions until Iteration 3, so thats why. 
Also, since the database is optional, no persistence is included.

When it comes to my module aka member 2. It is implemented by `AvailabilityService`, `AvailabilityRule`,
`RepetitionRule`, `AvailabilitySlot`, and `AvailabilityRepository`. Recurring weekly and monthly
rules create professional schedule intervals in memory. The service uses a `ServiceDurationProvider`
adapter to obtain appointment lengths from the shared service catalog while the integrated booking service
continues to filter candidates that conflict with existing appointments.

When it comes to member 3 (CSV import/export, CLI and integration):
- `csv` package: `AvailabilityCsvImporter.importAvailability(path)` and `AvailabilityCsvExporter.exportAvailability(path)`.
  The columns are the ones from the sample in the Iteration 2 instructions:
  `First Name,Last Name,Title,Specialty,Date,Start,End` (dates `yyyy-MM-dd`, times `HH:mm`).
  A missing file (`FILE_NOT_FOUND`) or wrong header (`INVALID_CSV_HEADER`) imports nothing; a bad row is skipped and
  reported by line number while the valid rows are imported. Exported files use the same columns, so they import back
  unchanged. Files are read as UTF-8 (accented names such as Amélie or Côté), with a Windows-1252 fallback for Excel files.
- `app` package: `ClinicCLI` (menu), `AppContext` (creates and wires every object), `InMemoryBookingCatalog`
  (implements `BookingCatalog`, which now also provides service durations as a `ServiceDurationProvider`),
  `ServiceCatalog` and `Service`.
- The system starts with no availability (load it with the CSV import or "Make availability"). A few demo patients and
  services are added at start-up because patient registration and service management are not part of this PoC.
- Small integration changes in `booking`: `bookAppointment` no longer takes `endsAt`, `BookingCatalog` lost
  `locationExists` and extends `ServiceDurationProvider`, and `HealthcareProfessional` has an optional `Specialty`
  so the CSV specialty column survives an import/export round trip.

