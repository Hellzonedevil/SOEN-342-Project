package booking;

import java.time.LocalDate;

// these are basically search fields that may be used to locate patients, from the Iteration 1 searchPatients contract
public class PatientSearchCriteria {
    private final String insuranceNumber;
    private final LocalDate dateOfBirth;
    private final String phoneNumber;
    private final String firstNamePart;
    private final String lastNamePart;

    public PatientSearchCriteria(String insuranceNumber, LocalDate dateOfBirth, String phoneNumber, String firstNamePart, String lastNamePart) {
        this.insuranceNumber = insuranceNumber;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.firstNamePart = firstNamePart;
        this.lastNamePart = lastNamePart;
    }

    private static boolean provided(String text) { //tiny helper function to determine if a string is provided (not null and not empty)
        return text != null && !text.trim().isEmpty();
    }

    public boolean isEmpty() { //basic logic to determine if teh search criterua is empty
        return !provided(insuranceNumber) && dateOfBirth == null && !provided(phoneNumber) && !provided(firstNamePart) && !provided(lastNamePart);
    }

    // this is basixally a function for finding matches when searching patients (i made sure that missing criteria do not restrict results)
    public boolean matches(Patient p) {
        return (!provided(insuranceNumber) || insuranceNumber.equalsIgnoreCase(p.getInsuranceNumber()))
                && (dateOfBirth == null || dateOfBirth.equals(p.getDateOfBirth()))
                && (!provided(phoneNumber) || phoneNumber.equals(p.getPhoneNumber()))
                && (!provided(firstNamePart) || contains(p.getFirstName(), firstNamePart))
                && (!provided(lastNamePart) || contains(p.getLastName(), lastNamePart));
    }

    private static boolean contains(String full, String part) { //tiny helper function to determine if a string contains another string, ignoring case and whitespace
        return full != null && full.toLowerCase(java.util.Locale.ROOT).contains(part.trim().toLowerCase(java.util.Locale.ROOT));
    }
}