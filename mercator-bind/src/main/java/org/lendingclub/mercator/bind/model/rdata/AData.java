/**
 * Copyright 2017 Lending Club, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.mercator.bind.model.rdata;


import java.util.HashMap;

import com.google.common.base.Preconditions;

public final class AData extends HashMap<String, Object> {

	  private static final long serialVersionUID = 1L;
	
	  public AData(String address) {
	    Preconditions.checkNotNull(address, "address");
	    Preconditions.checkArgument(address.indexOf('.') != -1, "%s should be a ipv4 address", address);
	    put("address", address);
	  }
	
	  public static AData create(String ipv4address) throws IllegalArgumentException {
	    return new AData(ipv4address);
	  }
	
	  public String address() {
	    return get("address").toString();
	  }
}
