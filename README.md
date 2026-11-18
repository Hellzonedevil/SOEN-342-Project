# SOEN-342-Project
Team Members: 
1) Sarvesh Sai Rajesh - 40231819 (Team Lead)
2) Alex Khoury - 40299086
3) Nicholas Nguyen - 40317237


To compile the current code:

```sh
javac -d out code/booking/*.java test/booking/*.java
java -cp out booking.AvailabilityServiceTest
```

Here's my module (as member 1 from our task division) with the 3 C1 entry points in BookingService:
1. `searchPatients(criteria)` - filters by the identifying criteria that we documented in Iteration 1
2. `findAvailableSlots(professionalId, serviceId, interval)` - delegates to Member 2's availability module and excludes already booked appointments
3. `bookAppointment(patientId, professionalId, serviceId, startsAt, endsAt, referralId)` - validates the references, rechecks the selected interval, checks required referrals, creates a `CONFIRMED` appointment and records a minimal booking audit event.


Extra Notes:
Iteration 2's new waitlist and predefined resource requirements are analysed (but not coded) by me, as Member #1, in the word document. The interation 2 instructions suggest deferring the design of these additions until Iteration 3, so thats why. 
Also, since the database is optional, no persistence is included.

When it comes to my module aka member 2. It is implemented by `AvailabilityService`, `AvailabilityRule`,
`RepetitionRule`, `AvailabilitySlot`, and `AvailabilityRepository`. Recurring weekly and monthly
rules create professional schedule intervals in memory. The service uses a `ServiceDurationProvider`
adapter to obtain appointment lengths from the shared service catalog while the integrated booking service
continues to filter candidates that conflict with existing appointments.