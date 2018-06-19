/* 
 * Copyright 2018 OpenSourceConsulting.
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
 *
 * Revision History
 * Author			Date				Description
 * ---------------	----------------	------------
 * Sang-cheon Park	Jun 11, 2018		First Draft.
 */
package com.osci.confluence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.osci.confluence.service.RemoveSpanService;

/**
 * <pre>
 * SpringBoot Main Class
 * </pre>
 * @author Sang-cheon Park
 * @version 1.0
 */
@SpringBootApplication
public class Application implements CommandLineRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(Application.class);
	
	@Autowired
	private RemoveSpanService removeSpanService;

	@Override
	public void run(String... args) throws Exception {
		Integer contentId = null;
		
		if (args.length == 1) {
			try {
				contentId = Integer.parseInt(args[0]);			
			} catch (Exception e) {
				logger.error("Can NOT parse argument for contentId.", e);
			}
		}
		
		removeSpanService.removeSpan(contentId);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
//end of Application.java