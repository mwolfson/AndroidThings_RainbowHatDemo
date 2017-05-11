package things.wolfsoft.com.androidthings;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by mwolfson on 5/11/17.
 */

public class Utilities {

    public static String getDate() {

        try {
            DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss", Locale.US);
            Date netDate = (new Date());
            return sdf.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }
}
