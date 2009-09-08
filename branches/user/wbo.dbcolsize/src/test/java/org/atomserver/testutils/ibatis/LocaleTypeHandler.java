/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
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

package org.atomserver.testutils.ibatis;

import java.util.Locale;

import java.sql.SQLException;

import org.apache.commons.lang.LocaleUtils;

import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.ParameterSetter;


public class  LocaleTypeHandler 
  implements TypeHandlerCallback 
{
  public Object getResult( ResultGetter getter ) 
    throws SQLException { 
      String val = getter.getString(); 
      if ( val == null ) {
          throw new SQLException( "NULL Result supplied in LocaleTypeHandler" );
      } else {
          return LocaleUtils.toLocale( val );
      }
  }
    
  public void setParameter(ParameterSetter setter, Object parameter) 
    throws SQLException { 
      String val = "undefined";
      if ( parameter != null ) {
          val = ((Locale)parameter).toString();
      }
      setter.setString( val );
  }

  public Object valueOf( String val ) 
  {  return LocaleUtils.toLocale( val ); }
} 

