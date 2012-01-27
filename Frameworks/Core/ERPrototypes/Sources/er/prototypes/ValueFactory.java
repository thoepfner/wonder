package er.prototypes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSPropertyListSerialization;

import er.extensions.foundation.ERXMutableArray;
import er.extensions.foundation.ERXMutableDictionary;

/**
 * ValueFactory provides static methods that produce EOAttribute values
 * from values stored in the database.
 */
public class ValueFactory {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(ValueFactory.class);
	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	
	public static Duration duration(String value) {
		try {
			Duration d = DatatypeFactory.newInstance().newDuration(value);
			return d;
		} catch (DatatypeConfigurationException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}
	
	public static LocalDate jodaLocalDate(Date value) {
		GregorianCalendar gc = new GregorianCalendar(GMT);
		gc.setTime(value);
		LocalDate ld = new LocalDate(gc.getTimeInMillis());
		return ld;
	}
	
	public static LocalDateTime jodaLocalDateTime(Date value) {
		GregorianCalendar gc = new GregorianCalendar(GMT);
		gc.setTime(value);
		LocalDateTime ldt = new LocalDateTime(gc.getTimeInMillis());
		return ldt;
	}
	
	public static LocalTime jodaLocalTime(Date value) {
		GregorianCalendar gc = new GregorianCalendar(GMT);
		gc.setTime(value);
		LocalTime time = new LocalTime(gc.getTimeInMillis());
		return time;
	}

	public static DateTime jodaDateTime(Date value) {
		DateTime dt = new DateTime(value.getTime());
		return dt;
	}
	
	@SuppressWarnings("rawtypes")
	public static NSArray stringArray(String value) {
		return (NSArray)NSPropertyListSerialization.propertyListFromString(value);
	}
	
	@SuppressWarnings("rawtypes")
	public static NSArray blobArray(NSData value) {
		return ERXMutableArray.fromBlob(value);
	}

	@SuppressWarnings("rawtypes")
	public static NSDictionary stringDictionary(String value) {
		return (NSDictionary) NSPropertyListSerialization.propertyListFromString(value);
	}
	
	@SuppressWarnings("rawtypes")
	public static NSDictionary blobDictionary(NSData value) {
		return ERXMutableDictionary.fromBlob(value);
	}
	
	public static Serializable serializable(byte[] value) {		
		ObjectInputStream ois = null;
		
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(value);
			ois = new ObjectInputStream(bais);
			Serializable obj = (Serializable)ois.readObject();
			if(obj instanceof Collection) {
				obj = (Serializable)Collections.unmodifiableCollection((Collection)obj);
			}
			return obj;
		} catch(IOException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		} catch(ClassNotFoundException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		} finally {
			try{ if(ois != null){ois.close();} } catch(IOException e) {}
		}
	}
}
