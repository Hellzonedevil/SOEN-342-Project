# SOEN-342-Project
Team Members: 
1) Sarvesh Sai Rajesh - 40231819 (Team Lead)
2) Alex Khoury - 40299086
3) Nicholas Nguyen - 40317237


To run the codee for now:

```sh
mkdir -p out
javac -d out booking/*.java
```

Here's my module (as member 1 from our task division) with the 3 C1 entry points in BookingService:
1. `searchPatients(criteria)` - filters by the identifying criteria that we documented in Iteration 1
2. `findAvailableSlots(professionalId, locationId, serviceId, interval)` - delegates to Member 2's availability module and excludes already booked appointments
3. `bookAppointment(patientId, professionalId, locationId, serviceId, startsAt, endsAt, referralId)` - validates the references, rechecks the selected interval, checks required referrals, creates a `CONFIRMED` appointment and records a minimal booking audit event.


Extra Notes:
Iteration 2's new waitlist and predefined resource requirements are analysed (but not coded) by me, as Member #1, in the word document. The interation 2 instructions suggest deferring the design of these additions until Iteration 3, so thats why. 
Also, since the database is optional, no persistence is included. 