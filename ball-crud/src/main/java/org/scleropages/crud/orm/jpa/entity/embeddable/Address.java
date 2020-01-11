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
package org.scleropages.crud.orm.jpa.entity.embeddable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Embeddable
public class Address {

	private int provinceId=0;
	private int cityId=0;
	private int districtId=0;
	private String addressDetail="unknown";

	@Column(name = "province_id")
	public int getProvinceId() {
		return provinceId;
	}

	@Column(name = "city_id")
	public int getCityId() {
		return cityId;
	}

	@Column(name = "district_id")
	public int getDistrictId() {
		return districtId;
	}

	@Column(name = "address_detail")
	public String getAddressDetail() {
		return addressDetail;
	}

	public void setProvinceId(int provinceId) {
		this.provinceId = provinceId;
	}

	public void setCityId(int cityId) {
		this.cityId = cityId;
	}

	public void setDistrictId(int districtId) {
		this.districtId = districtId;
	}

	public void setAddressDetail(String addressDetail) {
		this.addressDetail = addressDetail;
	}

}
