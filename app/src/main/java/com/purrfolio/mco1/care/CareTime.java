package com.purrfolio.mco1.care;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Resolves a confirmed local date/time without silently treating an ambiguous hour as AM. */
public final class CareTime {
    private CareTime(){}
    public static long parse(String date,String time) {
        String input=date.trim()+" "+time.trim().toUpperCase(Locale.US).replaceAll("\\s*(AM|PM)$"," $1");
        String[] formats={"yyyy-MM-dd h:mm a","yyyy-MM-dd h a","yyyy-MM-dd HH:mm"};
        // 7:30 requires an AM/PM choice; 00:30 or 19:30 is unambiguous 24-hour time.
        boolean military=time.trim().matches("(?:00|1[3-9]|2[0-3]):[0-5][0-9]");
        for(int i=0;i<formats.length;i++) {
            if(i==2&&!military)continue;
            SimpleDateFormat format=new SimpleDateFormat(formats[i],Locale.US);format.setLenient(false);
            ParsePosition p=new ParsePosition(0);Date parsed=format.parse(input,p);
            if(parsed!=null&&p.getIndex()==input.length())return parsed.getTime();
        }
        throw new IllegalArgumentException("Choose a valid date and a time with AM/PM (for example 7:30 AM), or use 00:00 / 13:00–23:59.");
    }
    public static String date(long time){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(time));}
    public static String time(long time){return new SimpleDateFormat("h:mm a",Locale.US).format(new Date(time));}
}
