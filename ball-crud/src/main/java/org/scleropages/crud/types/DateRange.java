/**
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
package org.scleropages.crud.types;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.scleropages.core.util.Dates;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.Date;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DateRange {

	public static final String DEFAULT_DATE_RANGE_SEPARATOR = " ~ ";

	private  final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public DateRange() {
	}

	public DateRange(Date startAfter, Date endBefore) {
		this();
		setStartAfter(startAfter);
		setEndBefore(endBefore);
	}

	public DateRange(String name, Date startAfter, Date endBefore) {
		this(startAfter, endBefore);
		this.name = name;
	}

	/**
	 * construct date range use default  {@link DateRange#DEFAULT_DATE_FORMAT}  and '~' as
	 * default separator
	 * 
	 * @param name
	 * @param dateRangeText
	 */
	public DateRange(String name, String dateRangeText) {

		String[] dateRange = StringUtils.split(dateRangeText, DEFAULT_DATE_RANGE_SEPARATOR);

		if (dateRange.length != 2)
			throw new IllegalArgumentException("invalid dateRangeText: " + dateRangeText);

		try {

			setStartAfter(DateUtils.parseDate(dateRange[0],DEFAULT_DATE_FORMAT));
			setEndBefore(DateUtils.parseDate(dateRange[1],DEFAULT_DATE_FORMAT));
		} catch (ParseException e) {
			throw new IllegalArgumentException("invalid dateRangeText: " + dateRangeText, e);

		}
		setName(name);
	}

	private String name;

	private Date startAfter;

	private Date endBefore;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStartAfter() {
		return startAfter;
	}

	private void setStartAfter(Date startAfter) {
		this.startAfter = startAfter;
		if (null != endBefore)
			Assert.isTrue(getDuration() > 0, "end-before must greater than start-after");
	}

	public Date getEndBefore() {
		return endBefore;
	}

	private void setEndBefore(Date endBefore) {
		this.endBefore = endBefore;
		if (null != startAfter)
			Assert.isTrue(getDuration() > 0, "end-before must greater than start-after");
	}

	public long getDuration() {
		if (null == startAfter && null != endBefore)
			return endBefore.getTime() - new Date().getTime();
		if (null == endBefore)
			return Long.MAX_VALUE;
		return endBefore.getTime() - startAfter.getTime();
	}

	public String getFormatDuration() {
		return Dates.getFormatDuration(getDuration());
	}

	public String asString() {
		return asString(DEFAULT_DATE_RANGE_SEPARATOR);
	}

	public String asString(String separator) {
		return asString(DEFAULT_DATE_FORMAT, separator);
	}

	public String asString(String format, String separator) {
		return DateFormatUtils.format(startAfter,format) + separator + DateFormatUtils.format(endBefore,format);
	}

	@Override
	public String toString() {
		return asString();
	}
}
