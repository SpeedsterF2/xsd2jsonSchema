import java.time.DateTimeException;
import java.time.LocalDate;

public class XsdDateConverter {

    public static LocalDate unmarshal(String value) throws DateTimeException {
        return LocalDate.parse(value);    }

    public static String marshal(LocalDate value) {
        return value.toString();
    }
}
