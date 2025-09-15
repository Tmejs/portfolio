package com.portfolio.demo.controller;

import com.portfolio.demo.repository.AbstractRepositoryIT;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Base class for controller integration tests.
 * Configures TestContainers with PostgreSQL and provides common test setup.
 */
@AutoConfigureMockMvc
public abstract class AbstractControllerIT extends AbstractRepositoryIT {

}
