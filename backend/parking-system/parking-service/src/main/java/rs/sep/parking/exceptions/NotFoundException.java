package rs.sep.parking.exceptions;

/** Mapped to 404. **/
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what) {
        super(what + " not found");
    }
}
