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

package ci.xlj.tools.jobflowcreator

import groovy.xml.StreamingMarkupBuilder;

import org.apache.log4j.Logger

import ci.xlj.libs.jenkinsvisitor.JenkinsVisitor
import ci.xlj.libs.utils.ConfigUtils
import ci.xlj.libs.utils.OSUtils
import ci.xlj.tools.jobflowcreator.config.ConfigLoader
import ci.xlj.tools.jobflowcreator.config.Globals

/**
 * This tools creates a job flow based on an existing given initial job.
 * 
 * @author kfzx-xulj
 *
 */
class JobFlowCreator {

	private Logger logger = Logger.getLogger(JobFlowCreator)

	private def showInfo() {
		println '''Job Flow Creator v1.0.0 of 4 Jul. 2014, by Mr. Xu Lijia.
Send bug reports via email icbcsdcpmoxulj@outlook.com
This tool is used to create a complete job flow on Jenkins Server at a time.\n'''
	}

	/**                                                                                                                                                 
	 * show help message                                                                                                                                
	 */                                                                                                                                                 
	private def showUsage() {
		println	'''Usage:
		   
1. Start from command line
   java -jar jobflowcreator.jar <First_Job_Name_in_the_Flow> <New_Value_for_the_Replaced_Segment>
OR
2. Called by pi-jobflowcreator plugin
   java -jar jobflowcreator.jar <First_Job_Name_in_the_Flow> 
                                <Job_Name_Segment_Pattern> <New_Value_for_the_Replaced_Segment> 
                                <Jenkins_Server_Url> <Username> <Password> 
                                <Jobs_directory>'''
	}

	static main(args) {
		new JobFlowCreator(args)
	}

	private String url
	private String username
	private String password
	private String jobsDir

	private JobFlowCreator(String[] args) {
		showInfo()

		if (!args) {
			showUsage()
			System.exit(0)
		}

		if (args.length == 7) {
			Globals.START_FROM_CMD = false

			url = args[3]
			username = args[4]
			password = args[5]
			jobsDir=args[6]

			init()
			createAJobFlow(args[0], args[1], args[2])

		} else if (args.length == 2) {
			Globals.START_FROM_CMD = true

			ConfigLoader.load()
			init()
			createAJobFlow(args[0], Globals.JOB_NAME_PATTERN,Globals.REPLACED_PATTERN)

		} else {
			println 'Invalid parameters. See usage for details.'
			showUsage()
			System.exit(-1)
		}

		println '\nProcess completed.'
	}


	private JenkinsVisitor v

	private void init() {
		boolean isLogin

		if (Globals.START_FROM_CMD) {
			v = new JenkinsVisitor(Globals.URL)
			isLogin = v.login(Globals.USERNAME, Globals.PASSWORD)
		} else {
			v = new JenkinsVisitor(url)
			isLogin = v.login(username,password)
		}

		if (!isLogin) {
			println v.responseContent
			System.exit(-2)
		}
	}

	//TODO: filter duplicate jobs
	private def newJobNames=new HashSet<String>()

	private def message=""

	private def createAJobFlow(firstJobName, jobNamePattern,replacement) {
		if (!v.getJobNameList().contains(firstJobName)) {
			println "Job ${firstJobName} does not exist."
			System.exit(-3)
		}

		createAJob(firstJobName, jobNamePattern, replacement)
	}

	private def createAJob(jobName,jobNamePattern,replacement) {
		def configXml=ConfigUtils.getConfigFile((Globals.START_FROM_CMD ? Globals.JOBS_DIR:jobsDir)
				+ File.separator + jobName)
		if(!configXml.exists()){
			message<<"Job ${jobName} does not exist."<<OSUtils.getOSLineSeparator()
			return
		}

		def xml=new XmlSlurper().parse(configXml)
		xml.publishers.'hudson.tasks.BuildTrigger'.childProjects=xml.publishers.'hudson.tasks.BuildTrigger'.childProjects.text().replaceAll(jobNamePattern, replacement)

		def newJobName=jobName.replaceFirst(jobNamePattern, replacement)
		def newXml=new StreamingMarkupBuilder()
		def result=v.create(newJobName,newXml.bindNode(xml).toString())
		if (result == 200) {
			def m="Job ${newJobName} created successfully."
			message<<m<<OSUtils.getOSLineSeparator()
			println m
			logger.info m
		} else {
			def m="Error in creating job ${newJobName}. See log for details."
			message<<m<<OSUtils.getOSLineSeparator()
			println m

			def res = v.responseContent
			if (res.contains('A job already exists')) {
				def n="Job ${newJobName} already exists."
				message<<n<<OSUtils.getOSLineSeparator()
				logger.error n
			} else {
				logger.error res
			}
		}

		def a=v.getDownStreamJobNameList(jobName)
		v.getDownStreamJobNameList(jobName).each {
			createAJobFlow(it, jobNamePattern,replacement)
		}
	}

}
