package it.unibz.inf.data_pumper.mappings;

/*
 * #%L
 * dataPumper
 * %%
 * Copyright (C) 2014 Free University of Bozen-Bolzano
 * %%
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
 * #L%
 */

import java.util.List;
import java.util.Set;

@Deprecated
public abstract class TupleTemplate {
	
	public abstract int getID();
	
	public abstract String getTemplatesString();
	
	public abstract Set<String>getReferredTables();
	
	public abstract List<String> getColumnsInTable(String tableName);
	
	public abstract int belongsToTuple();
	
	public abstract String toString();

	public abstract boolean equals(Object other);

	public abstract int hashCode();
}
