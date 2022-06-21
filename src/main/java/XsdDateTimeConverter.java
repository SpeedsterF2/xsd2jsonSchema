import java.time.DateTimeException;
import java.time.ZonedDateTime;

public class XsdDateTimeConverter {

    public static ZonedDateTime unmarshal(String value) throws DateTimeException {
        return ZonedDateTime.parse(value);
    }

    public static String marshal(ZonedDateTime value) {
        return value.toString();
    }
}
