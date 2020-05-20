package org.scleropages.core.util; /**
 * 
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Dates {

	public static final Date parse(XMLGregorianCalendar gregorianCalendar) {
		if (null == gregorianCalendar)
			return null;
		return gregorianCalendar.toGregorianCalendar().getTime();
	}

	public static final XMLGregorianCalendar parse(Date date) {
		if (null == date)
			return null;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	public static final XMLGregorianCalendar newXMLGregorianCalendar() {
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar();
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static final String getFormatDuration(Long duration){

		long diffDays = duration / (24 * 60 * 60 * 1000);
		long diffHours = duration / (60 * 60 * 1000) % 24;
		long diffMinutes = duration / (60 * 1000) % 60;
		long diffSeconds = duration / 1000 % 60;

		StringBuilder sb = new StringBuilder();

		sb.append(diffDays);
		sb.append(" Days,");
		sb.append(diffHours);
		sb.append(" Hours,");
		sb.append(diffMinutes);
		sb.append(" Minutes,");
		sb.append(diffSeconds);
		sb.append(" Seconds");

		return sb.toString();
	}
}
