package org.dspace.foresite;

import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.DateTime;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Created by IntelliJ IDEA.
 * User: richard
 * Date: Jun 16, 2008
 * Time: 12:12:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class DateParser
{
	// the possible date formats that we can support
    // See http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime
    // Specifically, dateTime is based on ISO 8601.  The JDK SimpleDateFormat timezone format ("Z") parses an RFC822 timezone (e.g. "-0500"), but
    // but not ISO 8601 timezones (e.g. "-05:00").

    public static final String timestamp = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String incompleteTimestamp = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public static Date parse(String str)
			throws OREParserException
	{
		return DateParser.parse(str, 0);
	}

	private static Date parse(String str, int type)
			throws OREParserException
	{
		try
		{
			SimpleDateFormat sdf;
			switch (type)
			{
				case 0:
					sdf = new SimpleDateFormat(timestamp);
					return sdf.parse(str);
				case 1:
					sdf = new SimpleDateFormat(incompleteTimestamp);
					return sdf.parse(str);
                case 2:
                    DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();
                    return dateTimeFormatter.parseDateTime(str).toDate();
                case 3:
                    DateTimeFormatter dateTimeFormatterNoMillis = ISODateTimeFormat.dateTimeNoMillis();
                    return dateTimeFormatterNoMillis.parseDateTime(str).toDate();
                default:
					throw new OREParserException("Unable to parse date: " + str);
			}
		}
		catch (ParseException e)
		{
			return DateParser.parse(str, ++type);
		}
        catch (IllegalArgumentException e)
        {
            return DateParser.parse(str, ++type);
        }
        catch (NullPointerException e)
        {
            // Should never happen
            return DateParser.parse(str, ++type);    
        }
    }
}
