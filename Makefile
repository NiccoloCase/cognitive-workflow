.PHONY: run e2e_test integration_test dev clean

# Run application
run:
	docker-compose --profile app up

# Run unit tests
unit_test:
	docker-compose run --rm dev mvn test

# Run end-to-end tests
e2e_test:
	docker-compose run --rm dev mvn failsafe:integration-test -Dit.test="*E2ETest"

# Run integration tests
integration_test:
	docker-compose run --rm dev mvn failsafe:integration-test -Dit.test="*IT"

# Run tests with focus tag
focus_test:
	docker-compose run --rm dev mvn test -Dgroups=focus
	docker-compose run --rm dev mvn failsafe:integration-test -Dgroups=focus -Dit.test="*IT"

# Run all tests
all_tests:
	make unit_test
	make integration_test
	make e2e_test

# Clean up
clean:
	docker-compose down --remove-orphans
	docker-compose --profile app down --remove-orphans
	docker-compose --profile dev down --remove-orphans
	docker-compose --profile e2e down --remove-orphans
	mvn clean