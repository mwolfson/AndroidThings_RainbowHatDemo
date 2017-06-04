package things.wolfsoft.com.androidthings;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    // Converts to celcius
    public static float convertFahrenheitToCelcius(float fahrenheit) {
        return ((fahrenheit - 32) * 5 / 9);
    }

    // Converts to fahrenheit
    public static  float convertCelciusToFahrenheit(float celsius) {
        return ((celsius * 9) / 5) + 32;
    }

}
