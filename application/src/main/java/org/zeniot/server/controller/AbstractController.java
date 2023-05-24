package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.zeniot.api.AccountService;
import org.zeniot.api.SimulatorService;
import org.zeniot.server.security.JwtHandler;

/**
 * @author Wu.Chunyang
 */
@Component
public abstract class AbstractController {

    @Autowired
    AccountService accountService;
    @Autowired
    UserDetailsService userDetailsService;
    @Autowired
    SimulatorService simulatorService;
    @Autowired
    JwtHandler jwtHandler;
    @Autowired
    PasswordEncoder passwordEncoder;

}
