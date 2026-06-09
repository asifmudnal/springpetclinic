/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.vet.Vets;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Full-stack integration tests that exercise the running application with the embedded
 * H2 database and real HTTP endpoints.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PetClinicWebIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	private RestTemplateBuilder builder;

	@Autowired
	private OwnerRepository owners;

	private RestTemplate template;

	@BeforeEach
	void setUp() {
		this.template = this.builder.rootUri("http://localhost:" + this.port).build();
	}

	@Test
	void testWelcomePage() {
		ResponseEntity<String> result = this.template.exchange(RequestEntity.get("/").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("Welcome");
	}

	@Test
	void testOwnerDetailsPage() {
		ResponseEntity<String> result = this.template.exchange(RequestEntity.get("/owners/1").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("Franklin").contains("Leo");
	}

	@Test
	void testVetsJsonEndpoint() {
		ResponseEntity<Vets> result = this.template.exchange(
				RequestEntity.get("/vets").accept(MediaType.APPLICATION_JSON).build(), Vets.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getVetList()).isNotEmpty();
		assertThat(result.getBody().getVetList().get(0).getLastName()).isEqualTo("Carter");
	}

	@Test
	void testVetsHtmlPage() {
		ResponseEntity<String> result = this.template.exchange(RequestEntity.get("/vets.html").build(),
				String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("James").contains("Carter");
	}

	@Test
	void testFindOwnerByLastNameRedirectsToOwnerDetails() {
		ResponseEntity<String> result = this.template.exchange(
				RequestEntity.get("/owners?lastName=Franklin").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("George").contains("Franklin");
	}

	@Test
	void testFindOwnersForm() {
		ResponseEntity<String> result = this.template.exchange(RequestEntity.get("/owners/find").build(),
				String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("Find Owners");
	}

	@Test
	void testCreateOwnerPersistsToDatabase() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("firstName", "Integration");
		form.add("lastName", "TestUser");
		form.add("address", "99 Test Lane");
		form.add("city", "Madison");
		form.add("telephone", "6085559999");

		ResponseEntity<String> result = this.template.exchange(
				RequestEntity.post("/owners/new").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form),
				String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("Integration").contains("TestUser");

		Page<Owner> savedOwners = this.owners.findByLastName("TestUser", PageRequest.of(0, 5));
		assertThat(savedOwners.getTotalElements()).isEqualTo(1);
	}

}
