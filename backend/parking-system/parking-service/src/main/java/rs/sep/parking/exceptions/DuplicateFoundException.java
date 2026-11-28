package rs.sep.parking.exceptions;

/** Mapped to 409. */
public class DuplicateFoundException extends RuntimeException {

    public DuplicateFoundException(String message) {
        super(message);
    }
}
