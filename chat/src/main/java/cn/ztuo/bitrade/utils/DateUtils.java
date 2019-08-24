package cn.ztuo.bitrade.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static final String FORMAT_COMMON = "yyyy-MM-dd HH:mm:ss" ;

    public static final String FORMAT_SHORT = "yyyy-MM-dd" ;

    public static SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_COMMON) ;

    public static String getDateStr(Date date){
        String dateStr = sdf.format(date);
        return dateStr ;
    }

    public static String getDateStr(long millis){
        String dateStr = getDateStr(new Date(millis));
        return dateStr ;
    }

    public static Date getDate(String dateStr){
        try {
            Date date = sdf.parse(dateStr);
            return date ;
        } catch (ParseException e) {
            e.printStackTrace();
            return null ;
        }

    }

    public static Date getCurrentDate(){
        return new Date();
    }

    public static String getCurrentDateStr(){
        return getDateStr(new Date());
    }
}
