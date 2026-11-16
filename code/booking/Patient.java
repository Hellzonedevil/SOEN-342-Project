package booking;

import java.time.LocalDate;

// this is basicallly a simple repersentation of a patient with identifiers and demographic values needed by the C1 patient search
public class Patient {
    private final String patientId;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final String insuranceNumber; // this one may be null for uninsured patients
    private final LocalDate dateOfBirth;

    public Patient(String patientId, String firstName, String lastName, String phoneNumber, String insuranceNumber, LocalDate dateOfBirth) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.insuranceNumber = insuranceNumber;
        this.dateOfBirth = dateOfBirth;
    }

    public String getPatientId() { return patientId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getInsuranceNumber() { return insuranceNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
}