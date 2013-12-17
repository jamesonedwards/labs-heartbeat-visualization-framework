package util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	public static final String[] DAY_SUFFIXES =
			  //    0     1     2     3     4     5     6     7     8     9
			     { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th",
			  //    10    11    12    13    14    15    16    17    18    19
			       "th", "th", "th", "th", "th", "th", "th", "th", "th", "th",
			  //    20    21    22    23    24    25    26    27    28    29
			       "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th",
			  //    30    31
			       "th", "st" };
	
	public static String getDaySuffix(Date date) throws IllegalArgumentException {
		if (date == null)
			throw new IllegalArgumentException("Date cannot be null!");
		SimpleDateFormat df = new SimpleDateFormat("d");
		int day = Integer.parseInt(df.format(date));
		return getDaySuffix(day);
	}

	public static String getDaySuffix(int day) {
		return DAY_SUFFIXES[day];
	}
}
