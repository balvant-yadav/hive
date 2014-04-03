package org.apache.hadoop.hive.ql.cube.metadata;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.api.FieldSchema;

public class InlineDimension extends BaseDimension {

  private final List<String> values;

  public InlineDimension(FieldSchema column, List<String> values) {
    this(column, null, null, null, values);
  }

  public InlineDimension(FieldSchema column, Date startTime, Date endTime,
      Double cost, List<String> values) {
    super(column, startTime, endTime, cost);
    this.values = values;
  }

  public List<String> getValues() {
    return values;
  }

  @Override
  public void addProperties(Map<String, String> props) {
    super.addProperties(props);
    props.put(MetastoreUtil.getInlineDimensionSizeKey(getName()),
        String.valueOf(values.size()));
    props.put(MetastoreUtil.getInlineDimensionValuesKey(getName()),
        MetastoreUtil.getStr(values));
  }

  /**
   * This is used only for serializing
   *
   * @param name
   * @param props
   */
  public InlineDimension(String name, Map<String, String> props) {
    super(name, props);
    String valueStr = props.get(MetastoreUtil.getInlineDimensionValuesKey(
        name));
    this.values = Arrays.asList(valueStr.split(","));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((getValues() == null) ? 0 :
        getValues().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    InlineDimension other = (InlineDimension) obj;
    if (this.getValues() == null) {
      if (other.getValues() != null) {
        return false;
      }
    } else if (!this.getValues().equals(other.getValues())) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    String str = super.toString();
    str += "values:" + MetastoreUtil.getStr(values);
    return str;
  }

}
