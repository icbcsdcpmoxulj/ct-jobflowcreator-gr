/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License") you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//      Contributors:      Xu Lijia

package ci.xlj.tools.jobflowcreator.config

/**
 * Load config from property file
 * 
 * @author Xu Lijia
 *
 */
class ConfigLoader {

	private static Properties props
	private static String JFC_HOME

	static void load() {
		JFC_HOME = System.getenv("JFC_HOME")
		if (JFC_HOME) {
			println "Please set system variable JFC_HOME and retry."
			System.exit(-1)
		}

		Globals.JFC_HOME = JFC_HOME

		props=new Properties()
		props.load(new FileReader("${JFC_HOME}/${Globals.CONFIG_FILE}"))

		Globals.URL = props.getProperty("URL")
		Globals.USERNAME = props.getProperty("USERNAME")
		Globals.PASSWORD = props.getProperty("PASSWORD")
		Globals.JOBS_DIR = props.getProperty("JOBS_DIR")
		Globals.JOB_NAME_PATTERN=props.getProperty("JOB_NAME_PATTERN")
		Globals.REPLACED_PATTERN=props.getProperty("REPLACED_PATTERN")
	}

}
