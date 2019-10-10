package dev.xframe.utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class XTimeFormatter {
	
	static DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static LocalDateTime toLocalDateTime(String t) {
		return LocalDateTime.parse(t, DATETIME_FORMATTER);
	}
	
	public static Timestamp toTimestamp(String t) {
		return Timestamp.valueOf(toLocalDateTime(t));
	}
	
	public static Date toDate(String t) {
		return Date.from(toLocalDateTime(t).atZone(ZoneId.systemDefault()).toInstant());
	}
	
	public static String from(Date t) {
		return from(t.toInstant());
	}
	
	public static String from(Timestamp t) {
		return from(t.toInstant());
	}
	
	public static String from(long n) {
		return from(Instant.ofEpochMilli(n));
	}
	
	public static String from(Instant instant) {
		return DATETIME_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
	}
	
}
